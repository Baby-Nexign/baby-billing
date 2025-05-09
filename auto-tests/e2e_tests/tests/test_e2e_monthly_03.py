import logging
from time import sleep
import psycopg


from database import (
    create_or_update_subscribers_with_related_data,
    get_sub_balance,
    get_quant_service_balance,
)
from rabbitmq_sender import send_cdr_list_to_rabbitmq
from subscriber_schema import SubscriberCreationData
from utils import calculate_billed_minutes


logger = logging.getLogger(__name__)

# --- Константы для теста E2E-MONTHLY-03 ---
# Абонент P2 (Помесячный, звонящий)
MSISDN_P2_M03 = "79222226662"
INITIAL_BALANCE_P2_M03 = 200
INITIAL_PACKAGE_MINUTES_P2_M03 = 3
TARIFF_ID_P2_M03 = 12
SERVICE_TYPE_ID_P2_PACKAGE_M03 = 0

# Вызываемый абонент (внешняя сеть)
MSISDN_EXTERNAL_CALLEE_M03 = "79888888888"

# CDR данные
CDR_CALL_TYPE_M03 = "01"
CDR_CALL_START_M03 = "2025-05-01T15:00:00"
CDR_CALL_END_M03 = "2025-05-01T15:04:59"

# Тарификация сверх пакета (на внешнюю сеть)
COST_PER_MINUTE_OVER_PACKAGE_EXTERNAL_M03 = 25

PAUSE_FOR_PROCESSING_S_M03 = 7


def test_e2e_monthly_03_partial_package_and_external_billing(db_connection: psycopg.Connection):
    """
    E2E-MONTHLY-03: ТП Помесячный, исходящий звонок на внешнюю сеть.
    Частичное списание из пакета, остаток - деньгами по тарифу для внешней сети.
    """
    subscriber_p2_data = SubscriberCreationData(
        msisdn=MSISDN_P2_M03,
        money=INITIAL_BALANCE_P2_M03,
        tariff_id_logical=TARIFF_ID_P2_M03,
        name_prefix="P2_Monthly03_",
        quant_s_type_id=SERVICE_TYPE_ID_P2_PACKAGE_M03,
        quant_amount_left=INITIAL_PACKAGE_MINUTES_P2_M03
    )

    subscribers_info = create_or_update_subscribers_with_related_data(
        db_connection, [subscriber_p2_data]  # Создаем только P2
    )

    p2_id = subscribers_info.get(MSISDN_P2_M03)
    assert p2_id is not None, f"Не удалось получить ID для абонента {MSISDN_P2_M03}"

    cdr_to_send = [{
        "callType": CDR_CALL_TYPE_M03,
        "firstSubscriberMsisdn": MSISDN_P2_M03,
        "secondSubscriberMsisdn": MSISDN_EXTERNAL_CALLEE_M03,  # Звонок на внешний номер
        "callStart": CDR_CALL_START_M03,
        "callEnd": CDR_CALL_END_M03
    }]
    assert send_cdr_list_to_rabbitmq(cdr_to_send), "Ошибка отправки CDR в RabbitMQ"

    call_duration_total_minutes = calculate_billed_minutes(CDR_CALL_START_M03, CDR_CALL_END_M03)
    assert call_duration_total_minutes == 5, \
        f"Расчетная общая длительность звонка ({call_duration_total_minutes} мин) не равна 5."

    sleep(PAUSE_FOR_PROCESSING_S_M03)

    expected_package_minutes_after = 0
    current_package_minutes_after = get_quant_service_balance(
        db_connection, p2_id, SERVICE_TYPE_ID_P2_PACKAGE_M03
    )

    assert current_package_minutes_after is not None, \
        f"Не удалось получить остаток пакетных минут для p_id {p2_id}"
    assert current_package_minutes_after == expected_package_minutes_after, \
        f"Остаток пакетных минут: {current_package_minutes_after}, ожидалось: {expected_package_minutes_after}"

    minutes_from_package_used = min(INITIAL_PACKAGE_MINUTES_P2_M03, call_duration_total_minutes)  # 3 минуты
    minutes_billed_from_money = call_duration_total_minutes - minutes_from_package_used  # 5 - 3 = 2 минуты

    cost_for_billed_minutes = minutes_billed_from_money * COST_PER_MINUTE_OVER_PACKAGE_EXTERNAL_M03  # 2 * 25 = 50

    expected_money_balance_after_p2 = INITIAL_BALANCE_P2_M03 - cost_for_billed_minutes  # 200 - 50 = 150
    current_money_balance_after_p2 = get_sub_balance(db_connection, MSISDN_P2_M03)

    assert current_money_balance_after_p2 is not None, \
        f"Не удалось получить денежный баланс для {MSISDN_P2_M03}"
    assert current_money_balance_after_p2 == expected_money_balance_after_p2, \
        f"Денежный баланс P2: {current_money_balance_after_p2}, ожидалось: {expected_money_balance_after_p2}"

    logger.info(
        f"Тест E2E-MONTHLY-03: Абонент P2 {MSISDN_P2_M03} (ID: {p2_id})\n"
        f"  Пакет минут (s_type_id={SERVICE_TYPE_ID_P2_PACKAGE_M03}) ДО: {INITIAL_PACKAGE_MINUTES_P2_M03}, ПОСЛЕ: {current_package_minutes_after} (ожидалось: {expected_package_minutes_after})\n"
        f"  Денежный баланс P2 ДО: {INITIAL_BALANCE_P2_M03}, ПОСЛЕ: {current_money_balance_after_p2} (ожидалось: {expected_money_balance_after_p2})"
    )
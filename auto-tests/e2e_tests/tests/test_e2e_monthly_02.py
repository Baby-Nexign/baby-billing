import logging
from time import sleep
import psycopg

from database import (
    create_or_update_subscribers_with_related_data,
    get_sub_balance, get_quant_service_balance,
)
from rabbitmq_sender import send_cdr_list_to_rabbitmq
from subscriber_schema import SubscriberCreationData
from utils import calculate_billed_minutes

logger = logging.getLogger(__name__)

# --- Константы для теста E2E-MONTHLY-02 ---
# Абонент P2 (Помесячный, звонящий)
MSISDN_P2_M02 = "79221234567"
INITIAL_BALANCE_P2_M02 = 200
INITIAL_PACKAGE_MINUTES_P2_M02 = 3
TARIFF_ID_P2_M02 = 12
SERVICE_TYPE_ID_P2_PACKAGE_M02 = 0

# Абонент P1 (Классика, принимающий)
MSISDN_P1_M02_CALLEE = "79340001122"
INITIAL_BALANCE_P1_M02_CALLEE = 50
TARIFF_ID_P1_M02_CALLEE = 11

# CDR данные
CDR_CALL_TYPE_M02 = "01"
CDR_CALL_START_M02 = "2025-05-01T14:00:00"
CDR_CALL_END_M02 = "2025-05-01T14:04:59"

COST_PER_MINUTE_OVER_PACKAGE_M02 = 15

PAUSE_FOR_PROCESSING_S_M02 = 7


def test_e2e_monthly_02_partial_package_deduction_and_billing(db_connection: psycopg.Connection):
    """
    E2E-MONTHLY-02: ТП Помесячный, исходящий внутрисетевой звонок.
    Частичное списание из пакета, остаток - деньгами.
    """
    subscriber_p2_data = SubscriberCreationData(
        msisdn=MSISDN_P2_M02,
        money=INITIAL_BALANCE_P2_M02,
        tariff_id_logical=TARIFF_ID_P2_M02,
        name_prefix="P2_Monthly02_",
        quant_s_type_id=SERVICE_TYPE_ID_P2_PACKAGE_M02,
        quant_amount_left=INITIAL_PACKAGE_MINUTES_P2_M02
    )
    subscriber_p1_data = SubscriberCreationData(
        msisdn=MSISDN_P1_M02_CALLEE,
        money=INITIAL_BALANCE_P1_M02_CALLEE,
        tariff_id_logical=TARIFF_ID_P1_M02_CALLEE,
        name_prefix="P1_ClassicM02_"
    )

    subscribers_info = create_or_update_subscribers_with_related_data(
        db_connection, [subscriber_p2_data, subscriber_p1_data]
    )

    p2_id = subscribers_info.get(MSISDN_P2_M02)
    assert p2_id is not None, f"Не удалось получить ID для абонента {MSISDN_P2_M02}"

    cdr_to_send = [{
        "callType": CDR_CALL_TYPE_M02,
        "firstSubscriberMsisdn": MSISDN_P2_M02,
        "secondSubscriberMsisdn": MSISDN_P1_M02_CALLEE,
        "callStart": CDR_CALL_START_M02,
        "callEnd": CDR_CALL_END_M02
    }]
    assert send_cdr_list_to_rabbitmq(cdr_to_send), "Ошибка отправки CDR в RabbitMQ"

    call_duration_total_minutes = calculate_billed_minutes(CDR_CALL_START_M02, CDR_CALL_END_M02)
    assert call_duration_total_minutes == 5, \
        f"Расчетная общая длительность звонка ({call_duration_total_minutes} мин) не равна 5."

    sleep(PAUSE_FOR_PROCESSING_S_M02)


    expected_package_minutes_after = 0
    current_package_minutes_after = get_quant_service_balance(
        db_connection, p2_id, SERVICE_TYPE_ID_P2_PACKAGE_M02
    )

    assert current_package_minutes_after is not None, \
        f"Не удалось получить остаток пакетных минут для p_id {p2_id}"
    assert current_package_minutes_after == expected_package_minutes_after, \
        f"Остаток пакетных минут: {current_package_minutes_after}, ожидалось: {expected_package_minutes_after}"

    minutes_from_package_used = min(INITIAL_PACKAGE_MINUTES_P2_M02,
                                    call_duration_total_minutes)  # Потрачено из пакета = 3
    minutes_billed_from_money = call_duration_total_minutes - minutes_from_package_used  # 5 - 3 = 2 минуты

    cost_for_billed_minutes = minutes_billed_from_money * COST_PER_MINUTE_OVER_PACKAGE_M02  # 2 * 15 = 30

    expected_money_balance_after_p2 = INITIAL_BALANCE_P2_M02 - cost_for_billed_minutes  # 200 - 30 = 170
    current_money_balance_after_p2 = get_sub_balance(db_connection, MSISDN_P2_M02)

    assert current_money_balance_after_p2 is not None, \
        f"Не удалось получить денежный баланс для {MSISDN_P2_M02}"
    assert current_money_balance_after_p2 == expected_money_balance_after_p2, \
        f"Денежный баланс P2: {current_money_balance_after_p2}, ожидалось: {expected_money_balance_after_p2}"

    current_money_balance_after_p1 = get_sub_balance(db_connection, MSISDN_P1_M02_CALLEE)
    assert current_money_balance_after_p1 == INITIAL_BALANCE_P1_M02_CALLEE, \
        f"Баланс P1 ({MSISDN_P1_M02_CALLEE}) изменился: {current_money_balance_after_p1}, хотя не должен был."

    logger.info(
        f"Тест E2E-MONTHLY-02: Абонент P2 {MSISDN_P2_M02} (ID: {p2_id})\n"
        f"  Пакет минут (s_type_id={SERVICE_TYPE_ID_P2_PACKAGE_M02}) ДО: {INITIAL_PACKAGE_MINUTES_P2_M02}, ПОСЛЕ: {current_package_minutes_after} (ожидалось: {expected_package_minutes_after})\n"
        f"  Денежный баланс P2 ДО: {INITIAL_BALANCE_P2_M02}, ПОСЛЕ: {current_money_balance_after_p2} (ожидалось: {expected_money_balance_after_p2})\n"
        f"  Баланс P1 ({MSISDN_P1_M02_CALLEE}) остался: {current_money_balance_after_p1} (ожидалось: {INITIAL_BALANCE_P1_M02_CALLEE})"
    )

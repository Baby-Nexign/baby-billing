import logging
from time import sleep

import psycopg

from database import create_or_update_subscribers_with_related_data, get_quant_service_balance, get_sub_balance
from rabbitmq_sender import send_cdr_list_to_rabbitmq
from subscriber_schema import SubscriberCreationData
from utils import calculate_billed_minutes

logger = logging.getLogger(__name__)

# --- Константы для теста E2E-MONTHLY-01 ---
MSISDN_MONTHLY_SUB = "79222222229"
MSISDN_EXTERNAL_CALLEE_MONTHLY = "79888888889"
INITIAL_BALANCE_MONTHLY = 20
INITIAL_PACKAGE_MINUTES = 40
SERVICE_TYPE_ID_FOR_MONTHLY_PACKAGE = 0

TARIFF_ID_MONTHLY = 12

CDR_CALL_TYPE_MONTHLY = "01"
CDR_CALL_START_MONTHLY = "2025-05-01T13:00:00"
CDR_CALL_END_MONTHLY = "2025-05-01T13:05:30"

PAUSE_FOR_PROCESSING_S_MONTHLY = 5


def test_e2e_monthly_01_package_minutes_deduction(db_connection: psycopg.Connection):
    """
    Проверка E2E-MONTHLY-01: ТП Помесячный, исходящий звонок в пределах пакета.
    Минуты списываются из пакета (s_type_id=0), деньги - нет.
    """

    monthly_subscriber_data = SubscriberCreationData(
        msisdn=MSISDN_MONTHLY_SUB,
        money=INITIAL_BALANCE_MONTHLY,
        tariff_id_logical=TARIFF_ID_MONTHLY,
        name_prefix="MonthlyE2E01_",
        quant_s_type_id=SERVICE_TYPE_ID_FOR_MONTHLY_PACKAGE,
        quant_amount_left=INITIAL_PACKAGE_MINUTES
    )

    subscribers_info = create_or_update_subscribers_with_related_data(
        db_connection, [monthly_subscriber_data]
    )
    assert MSISDN_MONTHLY_SUB in subscribers_info, f"Не удалось создать/обновить абонента {MSISDN_MONTHLY_SUB}"
    person_id_monthly_sub = subscribers_info[MSISDN_MONTHLY_SUB]

    cdr_to_send = [{
        "callType": CDR_CALL_TYPE_MONTHLY,
        "firstSubscriberMsisdn": MSISDN_MONTHLY_SUB,
        "secondSubscriberMsisdn": MSISDN_EXTERNAL_CALLEE_MONTHLY,
        "callStart": CDR_CALL_START_MONTHLY,
        "callEnd": CDR_CALL_END_MONTHLY
    }]
    assert send_cdr_list_to_rabbitmq(cdr_to_send), "Ошибка отправки CDR в RabbitMQ"

    billed_minutes_for_call = calculate_billed_minutes(CDR_CALL_START_MONTHLY, CDR_CALL_END_MONTHLY)
    sleep(PAUSE_FOR_PROCESSING_S_MONTHLY)

    expected_minutes_after = INITIAL_PACKAGE_MINUTES - billed_minutes_for_call
    current_minutes_after = get_quant_service_balance(
        db_connection,
        person_id_monthly_sub,
        SERVICE_TYPE_ID_FOR_MONTHLY_PACKAGE
    )

    assert current_minutes_after is not None, \
        f"Не удалось получить остаток пакетных минут для p_id {person_id_monthly_sub}, s_type_id {SERVICE_TYPE_ID_FOR_MONTHLY_PACKAGE}"
    assert current_minutes_after == expected_minutes_after, \
        f"Остаток пакетных минут (s_type_id={SERVICE_TYPE_ID_FOR_MONTHLY_PACKAGE}): {current_minutes_after}, ожидалось: {expected_minutes_after}"

    # 3.2. Проверка отсутствия изменения денежного баланса
    expected_money_balance_after = INITIAL_BALANCE_MONTHLY
    current_money_balance_after = get_sub_balance(db_connection, MSISDN_MONTHLY_SUB)

    assert current_money_balance_after is not None, \
        f"Не удалось получить денежный баланс для {MSISDN_MONTHLY_SUB}"
    assert current_money_balance_after == expected_money_balance_after, \
        f"Денежный баланс: {current_money_balance_after}, ожидалось: {expected_money_balance_after} (без изменений)"

    logger.info(
        f"Тест E2E-MONTHLY-01: Абонент {MSISDN_MONTHLY_SUB} (ID: {person_id_monthly_sub})\n"
        f"  Пакет минут ДО: {INITIAL_PACKAGE_MINUTES}, ПОСЛЕ: {current_minutes_after} (ожидалось: {expected_minutes_after})\n"
        f"  Денежный баланс ДО: {INITIAL_BALANCE_MONTHLY}, ПОСЛЕ: {current_money_balance_after} (ожидалось: {expected_money_balance_after})"
    )

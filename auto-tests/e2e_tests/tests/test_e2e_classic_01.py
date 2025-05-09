import logging
from time import sleep
import psycopg

from database import create_or_update_subscribers_with_related_data, get_sub_balance, connect_db
from rabbitmq_sender import send_cdr_list_to_rabbitmq
from subscriber_schema import SubscriberCreationData
from utils import calculate_billed_minutes

logger = logging.getLogger(__name__)

# --- Константы для теста E2E-CLASSIC-01 ----
MSISDN_CALLER = "79111111111"
MSISDN_CALLEE = "79333333333"
INITIAL_BALANCE_CALLER = 50
INITIAL_BALANCE_CALLEE = 60

CDR_CALL_TYPE = "01"
CDR_CALL_START = "2025-05-01T10:00:00"
CDR_CALL_END = "2025-05-01T10:03:45"

COST_PER_MINUTE = 15
DEFAULT_TARIFF_ID = 11

PAUSE_FOR_BILLING_S = 5


def test_e2e_classic_01(db_connection: psycopg.Connection):
    """
    Проверка E2E-CLASSIC-01: Внутрисетевой исходящий звонок, ТП Классика.
     Проверяет итоговое списание средств у обоих абонентов.
     """
    caller_data = SubscriberCreationData(
        msisdn=MSISDN_CALLER,
        money=INITIAL_BALANCE_CALLER,
        tariff_id_logical=DEFAULT_TARIFF_ID,
        name_prefix="CallerE2E_S_"
    )
    callee_data = SubscriberCreationData(
        msisdn=MSISDN_CALLEE,
        money=INITIAL_BALANCE_CALLEE,
        tariff_id_logical=DEFAULT_TARIFF_ID,
        name_prefix="CalleeE2E_S_"
    )

    # Создаем/изменяем абонентов
    create_or_update_subscribers_with_related_data(
        db_connection, [caller_data, callee_data]
    )
    cdr_to_send = [{
        "callType": CDR_CALL_TYPE,
        "firstSubscriberMsisdn": MSISDN_CALLER,
        "secondSubscriberMsisdn": MSISDN_CALLEE,
        "callStart": CDR_CALL_START,
        "callEnd": CDR_CALL_END
    }]
    assert send_cdr_list_to_rabbitmq(cdr_to_send), "Ошибка отправки CDR в RabbitMQ"

    # Расчет ожидаемой стоимости звонка
    expected_billed_minutes = calculate_billed_minutes(CDR_CALL_START, CDR_CALL_END)
    assert expected_billed_minutes == 4, f"Расчетная длительность звонка ({expected_billed_minutes} мин) не равна 4."

    call_cost = expected_billed_minutes * COST_PER_MINUTE

    sleep(PAUSE_FOR_BILLING_S)

    expected_caller_balance_after = INITIAL_BALANCE_CALLER - call_cost
    current_caller_balance_after = get_sub_balance(db_connection, MSISDN_CALLER)

    assert current_caller_balance_after is not None, f"Не удалось получить баланс для {MSISDN_CALLER}"
    assert current_caller_balance_after == expected_caller_balance_after, \
        f"Итоговый баланс вызывающего {MSISDN_CALLER}: {current_caller_balance_after}, ожидалось: {expected_caller_balance_after}"

    expected_callee_balance_after = INITIAL_BALANCE_CALLEE  # Баланс не меняется
    current_callee_balance_after = get_sub_balance(db_connection, MSISDN_CALLEE)

    assert current_callee_balance_after is not None, f"Не удалось получить баланс для {MSISDN_CALLEE}"
    assert current_callee_balance_after == expected_callee_balance_after, \
        f"Итоговый баланс вызываемого {MSISDN_CALLEE}: {current_callee_balance_after}, ожидалось: {expected_callee_balance_after}"

    logger.info(
        f"Баланс {MSISDN_CALLER} ДО: {INITIAL_BALANCE_CALLER}, ПОСЛЕ: {current_caller_balance_after} (ожидалось: {expected_caller_balance_after})")
    logger.info(
        f"Баланс {MSISDN_CALLEE} ДО: {INITIAL_BALANCE_CALLEE}, ПОСЛЕ: {current_callee_balance_after} (ожидалось: {expected_callee_balance_after})")

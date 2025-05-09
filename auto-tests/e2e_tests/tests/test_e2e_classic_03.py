import logging
from time import sleep
import psycopg

from database import create_or_update_subscribers_with_related_data, get_sub_balance, connect_db
from rabbitmq_sender import send_cdr_list_to_rabbitmq
from subscriber_schema import SubscriberCreationData
from utils import calculate_billed_minutes

logger = logging.getLogger(__name__)

# --- Константы для теста E2E-CLASSIC-03 ---
MSISDN_RECEIVER_E2E03 = "79111111111"
MSISDN_EXTERNAL_CALLER_E2E03 = "79888888888"
INITIAL_BALANCE_RECEIVER_E2E03 = 50

CDR_CALL_TYPE_E2E03 = "02"
CDR_CALL_START_E2E03 = "2025-05-01T12:00:00"
CDR_CALL_END_E2E03 = "2025-05-01T12:05:00"

COST_PER_MINUTE_INCOMING = 0
DEFAULT_TARIFF_ID_E2E03 = 11

PAUSE_FOR_PROCESSING_S_E2E03 = 5


def test_e2e_classic_03_incoming_call_no_debit(db_connection: psycopg.Connection):
    """
    Проверка E2E-CLASSIC-03: Входящий звонок, ТП Классика.
    Баланс не должен измениться.
    """

    receiver_data = SubscriberCreationData(
        msisdn=MSISDN_RECEIVER_E2E03,
        money=INITIAL_BALANCE_RECEIVER_E2E03,
        tariff_id_logical=DEFAULT_TARIFF_ID_E2E03,
        name_prefix="ReceiverE2E03_"
    )

    create_or_update_subscribers_with_related_data(
        db_connection, [receiver_data]
    )

    cdr_to_send = [{
        "callType": CDR_CALL_TYPE_E2E03,
        "firstSubscriberMsisdn": MSISDN_RECEIVER_E2E03,
        "secondSubscriberMsisdn": MSISDN_EXTERNAL_CALLER_E2E03,
        "callStart": CDR_CALL_START_E2E03,
        "callEnd": CDR_CALL_END_E2E03
    }]
    assert send_cdr_list_to_rabbitmq(cdr_to_send), "Ошибка отправки CDR в RabbitMQ"


    expected_billed_minutes = calculate_billed_minutes(CDR_CALL_START_E2E03, CDR_CALL_END_E2E03)
    assert expected_billed_minutes == 5, \
        f"Расчетная длительность звонка ({expected_billed_minutes} мин) не равна 5."

    call_cost = expected_billed_minutes * COST_PER_MINUTE_INCOMING
    assert call_cost == 0, f"Ожидаемая стоимость входящего звонка не равна 0, получили {call_cost}"

    sleep(PAUSE_FOR_PROCESSING_S_E2E03)

    # Баланс не должен измениться
    expected_receiver_balance_after = INITIAL_BALANCE_RECEIVER_E2E03 - call_cost
    current_receiver_balance_after = get_sub_balance(db_connection, MSISDN_RECEIVER_E2E03)

    assert current_receiver_balance_after is not None, \
        f"Не удалось получить баланс для {MSISDN_RECEIVER_E2E03}"
    assert current_receiver_balance_after == expected_receiver_balance_after, \
        f"Итоговый баланс принимающего {MSISDN_RECEIVER_E2E03}: {current_receiver_balance_after}, " \
        f"ожидалось: {expected_receiver_balance_after} (без изменений)"

    logger.info(
        f"Тест E2E-CLASSIC-03: Баланс {MSISDN_RECEIVER_E2E03} "
        f"ДО: {INITIAL_BALANCE_RECEIVER_E2E03}, ПОСЛЕ: {current_receiver_balance_after} "
        f"(ожидалось: {expected_receiver_balance_after})"
    )

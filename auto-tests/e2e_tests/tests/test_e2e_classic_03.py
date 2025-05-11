import logging
from time import sleep
import psycopg
import allure

from database import create_or_update_subscribers_with_related_data, get_sub_balance
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


@allure.epic("Биллинг")
@allure.feature("Тариф 'Классика'")
@allure.story("E2E-CLASSIC-03: Входящий звонок, баланс не меняется")
@allure.title("Проверка отсутствия списаний при входящем звонке (ТП 'Классика')")
@allure.description("""
    Тест проверяет, что баланс принимающего абонента на тарифном плане 'Классика'
    не изменяется при входящем звонке от внешнего абонента.
    Шаги:
    1. Создание/обновление данных принимающего абонента.
    2. Отправка CDR о входящем звонке в RabbitMQ.
    3. Проверка, что расчетная стоимость входящего звонка равна нулю.
    4. Проверка баланса принимающего абонента (не должен измениться).
""")
@allure.severity(allure.severity_level.NORMAL)
def test_e2e_classic_03_incoming_call_no_debit(db_connection: psycopg.Connection):
    """
    Проверка E2E-CLASSIC-03: Входящий звонок, ТП Классика.
    Баланс не должен измениться.
    """

    with allure.step("1. Подготовка данных принимающего абонента"):
        receiver_data = SubscriberCreationData(
            msisdn=MSISDN_RECEIVER_E2E03,
            money=INITIAL_BALANCE_RECEIVER_E2E03,
            tariff_id_logical=DEFAULT_TARIFF_ID_E2E03,
            name_prefix="ReceiverE2E03_"
        )
        allure.attach(receiver_data.model_dump_json(indent=2), name="Данные принимающего (вход)", attachment_type=allure.attachment_type.JSON)

        create_or_update_subscribers_with_related_data(
            db_connection, [receiver_data]
        )

    with allure.step(f"2. Отправка CDR (входящий звонок): {MSISDN_EXTERNAL_CALLER_E2E03} -> {MSISDN_RECEIVER_E2E03}, Начало: {CDR_CALL_START_E2E03}, Конец: {CDR_CALL_END_E2E03}"):
        cdr_to_send = [{
            "callType": CDR_CALL_TYPE_E2E03,
            "firstSubscriberMsisdn": MSISDN_RECEIVER_E2E03, # Для входящего CDR, первый MSISDN - это наш абонент, принимающий звонок
            "secondSubscriberMsisdn": MSISDN_EXTERNAL_CALLER_E2E03, # Второй MSISDN - это тот, кто звонит (внешний номер)
            "callStart": CDR_CALL_START_E2E03,
            "callEnd": CDR_CALL_END_E2E03
        }]
        allure.attach(str(cdr_to_send), name="Отправляемый CDR (входящий)", attachment_type=allure.attachment_type.JSON)
        assert send_cdr_list_to_rabbitmq(cdr_to_send), "Ошибка отправки CDR в RabbitMQ"


    with allure.step("3. Проверка расчетной стоимости входящего звонка"):
        expected_billed_minutes = calculate_billed_minutes(CDR_CALL_START_E2E03, CDR_CALL_END_E2E03)
        allure.attach(f"Расчетные биллинговые минуты: {expected_billed_minutes}", name="Расчет минут", attachment_type=allure.attachment_type.TEXT)
        assert expected_billed_minutes == 5, \
            f"Расчетная длительность звонка ({expected_billed_minutes} мин) не равна 5."

        call_cost = expected_billed_minutes * COST_PER_MINUTE_INCOMING
        allure.attach(f"Стоимость минуты (входящий): {COST_PER_MINUTE_INCOMING}\nОжидаемая стоимость звонка: {call_cost}", name="Расчет стоимости", attachment_type=allure.attachment_type.TEXT)
        assert call_cost == 0, f"Ожидаемая стоимость входящего звонка не равна 0, получили {call_cost}"

    with allure.step(f"Пауза {PAUSE_FOR_PROCESSING_S_E2E03} сек. для обработки"):
        sleep(PAUSE_FOR_PROCESSING_S_E2E03)

    with allure.step(f"4. Проверка баланса принимающего абонента ({MSISDN_RECEIVER_E2E03}) после входящего звонка"):
        # Баланс не должен измениться, т.к. call_cost равен 0
        expected_receiver_balance_after = INITIAL_BALANCE_RECEIVER_E2E03 - call_cost
        current_receiver_balance_after = get_sub_balance(db_connection, MSISDN_RECEIVER_E2E03)

        allure.attach(
            f"Начальный баланс: {INITIAL_BALANCE_RECEIVER_E2E03}\n"
            f"Списано (ожидается 0): {call_cost}\n"
            f"Ожидаемый баланс: {expected_receiver_balance_after}\n"
            f"Текущий баланс: {current_receiver_balance_after}",
            name=f"Детали баланса {MSISDN_RECEIVER_E2E03}",
            attachment_type=allure.attachment_type.TEXT
        )

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
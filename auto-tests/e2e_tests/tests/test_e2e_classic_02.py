import logging
from time import sleep
import psycopg
import allure

from database import create_or_update_subscribers_with_related_data, get_sub_balance
from rabbitmq_sender import send_cdr_list_to_rabbitmq
from subscriber_schema import SubscriberCreationData
from utils import calculate_billed_minutes

logger = logging.getLogger(__name__)

# --- Константы для теста E2E-CLASSIC-02 ---
MSISDN_CALLER_E2E02 = "79111111111"
MSISDN_EXTERNAL_CALLEE_E2E02 = "79888888888"
INITIAL_BALANCE_CALLER_E2E02 = 50

CDR_CALL_TYPE_E2E02 = "01"
CDR_CALL_START_E2E02 = "2025-05-01T11:00:00"
CDR_CALL_END_E2E02 = "2025-05-01T11:01:10"

COST_PER_MINUTE_EXTERNAL = 25
DEFAULT_TARIFF_ID_E2E02 = 11

PAUSE_FOR_BILLING_S_E2E02 = 5


@allure.epic("Биллинг")
@allure.feature("Тариф 'Классика'")
@allure.story("E2E-CLASSIC-02: Исходящий звонок на другого оператора, корректное списание")
@allure.title("Проверка списания средств при звонке на внешнего оператора (ТП 'Классика')")
@allure.description("""
    Тест проверяет корректность списания средств у звонящего абонента
    при исходящем звонке на номер другого (внешнего) оператора
    для тарифного плана 'Классика'.
    Шаги:
    1. Создание/обновление данных звонящего абонента.
    2. Отправка CDR о звонке на внешний номер в RabbitMQ.
    3. Расчет ожидаемой стоимости звонка по тарифу для внешних вызовов.
    4. Проверка баланса звонящего абонента после биллинга.
""")
@allure.severity(allure.severity_level.CRITICAL)
def test_e2e_classic_02_external_call_debiting(db_connection: psycopg.Connection):
    """
    Проверка E2E-CLASSIC-02: Исходящий звонок на другого оператора, ТП Классика.
    Проверяет итоговое списание средств у вызывающего абонента.
    """

    with allure.step("1. Подготовка данных звонящего абонента"):
        caller_data = SubscriberCreationData(
            msisdn=MSISDN_CALLER_E2E02,
            money=INITIAL_BALANCE_CALLER_E2E02,
            tariff_id_logical=DEFAULT_TARIFF_ID_E2E02,
            name_prefix="CallerE2E02_"
        )
        allure.attach(caller_data.model_dump_json(indent=2), name="Данные звонящего (вход)", attachment_type=allure.attachment_type.JSON)

        create_or_update_subscribers_with_related_data(
            db_connection, [caller_data]
        )

    with allure.step(f"2. Отправка CDR: {MSISDN_CALLER_E2E02} -> {MSISDN_EXTERNAL_CALLEE_E2E02} (внешний), Начало: {CDR_CALL_START_E2E02}, Конец: {CDR_CALL_END_E2E02}"):
        cdr_to_send = [{
            "callType": CDR_CALL_TYPE_E2E02,
            "firstSubscriberMsisdn": MSISDN_CALLER_E2E02,
            "secondSubscriberMsisdn": MSISDN_EXTERNAL_CALLEE_E2E02,  # Внешний номер
            "callStart": CDR_CALL_START_E2E02,
            "callEnd": CDR_CALL_END_E2E02
        }]
        allure.attach(str(cdr_to_send), name="Отправляемый CDR (внешний звонок)", attachment_type=allure.attachment_type.JSON)
        assert send_cdr_list_to_rabbitmq(cdr_to_send), "Ошибка отправки CDR в RabbitMQ"

    with allure.step("3. Расчет ожидаемой стоимости звонка (внешний оператор)"):
        expected_billed_minutes = calculate_billed_minutes(CDR_CALL_START_E2E02, CDR_CALL_END_E2E02)
        allure.attach(f"Ожидаемые биллинговые минуты: {expected_billed_minutes}", name="Расчет минут", attachment_type=allure.attachment_type.TEXT)
        assert expected_billed_minutes == 2, \
            f"Расчетная длительность звонка ({expected_billed_minutes} мин) не равна 2."

        call_cost = expected_billed_minutes * COST_PER_MINUTE_EXTERNAL # Используем константу для внешних звонков
        allure.attach(f"Стоимость минуты (внешний): {COST_PER_MINUTE_EXTERNAL}\nОжидаемая стоимость звонка: {call_cost}", name="Расчет стоимости", attachment_type=allure.attachment_type.TEXT)

    with allure.step(f"Пауза {PAUSE_FOR_BILLING_S_E2E02} сек. для обработки биллинга"):
        sleep(PAUSE_FOR_BILLING_S_E2E02)

    with allure.step(f"4. Проверка баланса звонящего абонента ({MSISDN_CALLER_E2E02}) после внешнего звонка"):
        expected_caller_balance_after = INITIAL_BALANCE_CALLER_E2E02 - call_cost
        current_caller_balance_after = get_sub_balance(db_connection, MSISDN_CALLER_E2E02)

        allure.attach(
            f"Начальный баланс: {INITIAL_BALANCE_CALLER_E2E02}\n"
            f"Списано за внешний звонок: {call_cost}\n"
            f"Ожидаемый баланс: {expected_caller_balance_after}\n"
            f"Текущий баланс: {current_caller_balance_after}",
            name=f"Детали баланса {MSISDN_CALLER_E2E02}",
            attachment_type=allure.attachment_type.TEXT
        )

        assert current_caller_balance_after is not None, \
            f"Не удалось получить баланс для {MSISDN_CALLER_E2E02}"
        assert current_caller_balance_after == expected_caller_balance_after, \
            f"Итоговый баланс вызывающего {MSISDN_CALLER_E2E02}: {current_caller_balance_after}, " \
            f"ожидалось: {expected_caller_balance_after}"

    logger.info(
        f"Баланс {MSISDN_CALLER_E2E02} ДО: {INITIAL_BALANCE_CALLER_E2E02}, ПОСЛЕ: {current_caller_balance_after} (ожидалось: {expected_caller_balance_after})"
    )

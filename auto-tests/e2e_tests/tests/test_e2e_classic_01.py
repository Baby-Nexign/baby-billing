import logging
from time import sleep
import psycopg
import allure # <--- Импортируем allure

from database import create_or_update_subscribers_with_related_data, get_sub_balance
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


@allure.epic("Биллинг")
@allure.feature("Тариф 'Классика'")
@allure.story("E2E-CLASSIC-01: Внутрисетевой исходящий звонок, корректное списание")
@allure.title("Проверка списания средств при внутрисетевом звонке на тарифе 'Классика'")
@allure.description("""
    Тест проверяет корректность списания средств у звонящего абонента
    и неизменность баланса у принимающего абонента при внутрисетевом звонке
    на тарифном плане 'Классика'.
    Шаги:
    1. Создание/обновление данных звонящего и принимающего абонентов.
    2. Отправка CDR о звонке в RabbitMQ.
    3. Расчет ожидаемой стоимости звонка.
    4. Проверка баланса звонящего абонента после биллинга.
    5. Проверка баланса принимающего абонента (не должен измениться).
""")
@allure.severity(allure.severity_level.CRITICAL)
def test_e2e_classic_01(db_connection: psycopg.Connection):
    """
    Проверка E2E-CLASSIC-01: Внутрисетевой исходящий звонок, ТП Классика.
     Проверяет итоговое списание средств у обоих абонентов.
     """
    with allure.step("1. Подготовка данных абонентов (звонящий и принимающий)"):
        caller_data = SubscriberCreationData(
            msisdn=MSISDN_CALLER,
            money=INITIAL_BALANCE_CALLER,
            tariff_id_logical=DEFAULT_TARIFF_ID,
            name_prefix="CallerE2E01_" # Обновил префикс для уникальности, если нужно
        )
        callee_data = SubscriberCreationData(
            msisdn=MSISDN_CALLEE,
            money=INITIAL_BALANCE_CALLEE,
            tariff_id_logical=DEFAULT_TARIFF_ID,
            name_prefix="CalleeE2E01_" # Обновил префикс
        )
        allure.attach(caller_data.model_dump_json(indent=2), name="Данные звонящего абонента (вход)", attachment_type=allure.attachment_type.JSON)
        allure.attach(callee_data.model_dump_json(indent=2), name="Данные принимающего абонента (вход)", attachment_type=allure.attachment_type.JSON)

        # Создаем/изменяем абонентов
        create_or_update_subscribers_with_related_data(
            db_connection, [caller_data, callee_data]
        )

    with allure.step(f"2. Отправка CDR: {MSISDN_CALLER} -> {MSISDN_CALLEE}, Начало: {CDR_CALL_START}, Конец: {CDR_CALL_END}"):
        cdr_to_send = [{
            "callType": CDR_CALL_TYPE,
            "firstSubscriberMsisdn": MSISDN_CALLER,
            "secondSubscriberMsisdn": MSISDN_CALLEE,
            "callStart": CDR_CALL_START,
            "callEnd": CDR_CALL_END
        }]
        allure.attach(str(cdr_to_send), name="Отправляемый CDR", attachment_type=allure.attachment_type.JSON)
        assert send_cdr_list_to_rabbitmq(cdr_to_send), "Ошибка отправки CDR в RabbitMQ"

    with allure.step("3. Расчет ожидаемой стоимости звонка"):
        # Расчет ожидаемой стоимости звонка
        expected_billed_minutes = calculate_billed_minutes(CDR_CALL_START, CDR_CALL_END)
        allure.attach(f"Ожидаемые биллинговые минуты: {expected_billed_minutes}", name="Расчет минут", attachment_type=allure.attachment_type.TEXT)
        assert expected_billed_minutes == 4, f"Расчетная длительность звонка ({expected_billed_minutes} мин) не равна 4."

        call_cost = expected_billed_minutes * COST_PER_MINUTE
        allure.attach(f"Стоимость минуты (внутрисеть): {COST_PER_MINUTE}\nОжидаемая стоимость звонка: {call_cost}", name="Расчет стоимости", attachment_type=allure.attachment_type.TEXT)


    with allure.step(f"Пауза {PAUSE_FOR_BILLING_S} сек. для обработки биллинга"):
        sleep(PAUSE_FOR_BILLING_S)

    with allure.step(f"4. Проверка баланса звонящего абонента ({MSISDN_CALLER})"):
        expected_caller_balance_after = INITIAL_BALANCE_CALLER - call_cost
        current_caller_balance_after = get_sub_balance(db_connection, MSISDN_CALLER)

        allure.attach(
            f"Начальный баланс: {INITIAL_BALANCE_CALLER}\n"
            f"Списано: {call_cost}\n"
            f"Ожидаемый баланс: {expected_caller_balance_after}\n"
            f"Текущий баланс: {current_caller_balance_after}",
            name=f"Детали баланса {MSISDN_CALLER}",
            attachment_type=allure.attachment_type.TEXT
        )
        assert current_caller_balance_after is not None, f"Не удалось получить баланс для {MSISDN_CALLER}"
        assert current_caller_balance_after == expected_caller_balance_after, \
            f"Итоговый баланс вызывающего {MSISDN_CALLER}: {current_caller_balance_after}, ожидалось: {expected_caller_balance_after}"

    with allure.step(f"5. Проверка баланса принимающего абонента ({MSISDN_CALLEE})"):
        expected_callee_balance_after = INITIAL_BALANCE_CALLEE
        current_callee_balance_after = get_sub_balance(db_connection, MSISDN_CALLEE)

        allure.attach(
            f"Начальный баланс: {INITIAL_BALANCE_CALLEE}\n"
            f"Списано (ожидается 0): 0\n" 
            f"Ожидаемый баланс: {expected_callee_balance_after}\n"
            f"Текущий баланс: {current_callee_balance_after}",
            name=f"Детали баланса {MSISDN_CALLEE}",
            attachment_type=allure.attachment_type.TEXT
        )
        assert current_callee_balance_after is not None, f"Не удалось получить баланс для {MSISDN_CALLEE}"
        assert current_callee_balance_after == expected_callee_balance_after, \
            f"Итоговый баланс вызываемого {MSISDN_CALLEE}: {current_callee_balance_after}, ожидалось: {expected_callee_balance_after}"

    logger.info(
        f"Баланс {MSISDN_CALLER} ДО: {INITIAL_BALANCE_CALLER}, ПОСЛЕ: {current_caller_balance_after} (ожидалось: {expected_caller_balance_after})")
    logger.info(
        f"Баланс {MSISDN_CALLEE} ДО: {INITIAL_BALANCE_CALLEE}, ПОСЛЕ: {current_callee_balance_after} (ожидалось: {expected_callee_balance_after})")
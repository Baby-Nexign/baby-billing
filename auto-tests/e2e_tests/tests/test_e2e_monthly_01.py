import logging
from time import sleep
import psycopg
import allure
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


@allure.epic("Биллинг")
@allure.feature("Тариф 'Помесячный'")
@allure.story("E2E-MONTHLY-01: Исходящий звонок в пределах пакета минут")
@allure.title("Проверка списания минут из пакета (ТП 'Помесячный') без изменения денежного баланса")
@allure.description("""
    Тест проверяет, что при исходящем звонке абонента с тарифным планом 'Помесячный':
    1. Минуты корректно списываются из предоплаченного пакета.
    2. Денежный баланс абонента не изменяется, если длительность звонка не превышает остаток минут в пакете.
    Шаги:
    1. Создание/обновление данных абонента с начальным пакетом минут и денежным балансом.
    2. Отправка CDR о звонке в RabbitMQ.
    3. Расчет ожидаемого остатка минут в пакете после звонка.
    4. Проверка фактического остатка минут в пакете.
    5. Проверка неизменности денежного баланса абонента.
""")
@allure.severity(allure.severity_level.CRITICAL)
def test_e2e_monthly_01_package_minutes_deduction(db_connection: psycopg.Connection):
    """
    Проверка E2E-MONTHLY-01: ТП Помесячный, исходящий звонок в пределах пакета.
    Минуты списываются из пакета (s_type_id=0), деньги - нет.
    """

    with allure.step("1. Подготовка данных абонента с пакетом минут"):
        monthly_subscriber_data = SubscriberCreationData(
            msisdn=MSISDN_MONTHLY_SUB,
            money=INITIAL_BALANCE_MONTHLY,
            tariff_id_logical=TARIFF_ID_MONTHLY,
            name_prefix="MonthlyE2E01_",
            quant_s_type_id=SERVICE_TYPE_ID_FOR_MONTHLY_PACKAGE,
            quant_amount_left=INITIAL_PACKAGE_MINUTES
        )
        allure.attach(monthly_subscriber_data.model_dump_json(indent=2), name="Данные абонента (вход)",
                      attachment_type=allure.attachment_type.JSON)

        subscribers_info = create_or_update_subscribers_with_related_data(
            db_connection, [monthly_subscriber_data]
        )
        assert MSISDN_MONTHLY_SUB in subscribers_info, f"Не удалось создать/обновить абонента {MSISDN_MONTHLY_SUB}"
        person_id_monthly_sub = subscribers_info[MSISDN_MONTHLY_SUB]
        allure.attach(f"ID абонента: {person_id_monthly_sub}", name="ID созданного/обновленного абонента",
                      attachment_type=allure.attachment_type.TEXT)

    with allure.step(
            f"2. Отправка CDR: {MSISDN_MONTHLY_SUB} -> {MSISDN_EXTERNAL_CALLEE_MONTHLY}, Начало: {CDR_CALL_START_MONTHLY}, Конец: {CDR_CALL_END_MONTHLY}"):
        cdr_to_send = [{
            "callType": CDR_CALL_TYPE_MONTHLY,
            "firstSubscriberMsisdn": MSISDN_MONTHLY_SUB,
            "secondSubscriberMsisdn": MSISDN_EXTERNAL_CALLEE_MONTHLY,
            "callStart": CDR_CALL_START_MONTHLY,
            "callEnd": CDR_CALL_END_MONTHLY
        }]
        allure.attach(str(cdr_to_send), name="Отправляемый CDR", attachment_type=allure.attachment_type.JSON)
        assert send_cdr_list_to_rabbitmq(cdr_to_send), "Ошибка отправки CDR в RabbitMQ"

    with allure.step("3. Расчет биллинговых минут и ожидаемого остатка в пакете"):
        billed_minutes_for_call = calculate_billed_minutes(CDR_CALL_START_MONTHLY, CDR_CALL_END_MONTHLY)
        allure.attach(f"Биллинговые минуты за звонок: {billed_minutes_for_call}", name="Расчет биллинговых минут",
                      attachment_type=allure.attachment_type.TEXT)

        expected_minutes_after = INITIAL_PACKAGE_MINUTES - billed_minutes_for_call
        allure.attach(
            f"Начальный пакет минут: {INITIAL_PACKAGE_MINUTES}\n"
            f"Списано минут: {billed_minutes_for_call}\n"
            f"Ожидаемый остаток минут: {expected_minutes_after}",
            name="Расчет остатка пакетных минут",
            attachment_type=allure.attachment_type.TEXT
        )

    with allure.step(f"Пауза {PAUSE_FOR_PROCESSING_S_MONTHLY} сек. для обработки биллинга"):
        sleep(PAUSE_FOR_PROCESSING_S_MONTHLY)

    with allure.step(
            f"4. Проверка остатка пакетных минут для абонента {MSISDN_MONTHLY_SUB} (ID: {person_id_monthly_sub})"):
        current_minutes_after = get_quant_service_balance(
            db_connection,
            person_id_monthly_sub,
            SERVICE_TYPE_ID_FOR_MONTHLY_PACKAGE
        )
        allure.attach(
            f"Ожидаемый остаток: {expected_minutes_after}\n"
            f"Текущий остаток: {current_minutes_after}",
            name=f"Детали пакетных минут {MSISDN_MONTHLY_SUB}",
            attachment_type=allure.attachment_type.TEXT
        )
        assert current_minutes_after is not None, \
            f"Не удалось получить остаток пакетных минут для p_id {person_id_monthly_sub}, s_type_id {SERVICE_TYPE_ID_FOR_MONTHLY_PACKAGE}"
        assert current_minutes_after == expected_minutes_after, \
            f"Остаток пакетных минут (s_type_id={SERVICE_TYPE_ID_FOR_MONTHLY_PACKAGE}): {current_minutes_after}, ожидалось: {expected_minutes_after}"

    with allure.step(f"5. Проверка денежного баланса абонента {MSISDN_MONTHLY_SUB} (не должен измениться)"):
        expected_money_balance_after = INITIAL_BALANCE_MONTHLY  # Деньги не должны списаться
        current_money_balance_after = get_sub_balance(db_connection, MSISDN_MONTHLY_SUB)

        allure.attach(
            f"Начальный денежный баланс: {INITIAL_BALANCE_MONTHLY}\n"
            f"Ожидаемый денежный баланс: {expected_money_balance_after}\n"
            f"Текущий денежный баланс: {current_money_balance_after}",
            name=f"Детали денежного баланса {MSISDN_MONTHLY_SUB}",
            attachment_type=allure.attachment_type.TEXT
        )
        assert current_money_balance_after is not None, \
            f"Не удалось получить денежный баланс для {MSISDN_MONTHLY_SUB}"
        assert current_money_balance_after == expected_money_balance_after, \
            f"Денежный баланс: {current_money_balance_after}, ожидалось: {expected_money_balance_after} (без изменений)"

    logger.info(
        f"Тест E2E-MONTHLY-01: Абонент {MSISDN_MONTHLY_SUB} (ID: {person_id_monthly_sub})\n"
        f"  Пакет минут ДО: {INITIAL_PACKAGE_MINUTES}, ПОСЛЕ: {current_minutes_after} (ожидалось: {expected_minutes_after})\n"
        f"  Денежный баланс ДО: {INITIAL_BALANCE_MONTHLY}, ПОСЛЕ: {current_money_balance_after} (ожидалось: {expected_money_balance_after})"
    )
import logging
from time import sleep
import psycopg
import allure  # <--- Импортируем allure

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

COST_PER_MINUTE_OVER_PACKAGE_EXTERNAL_M03 = 25

PAUSE_FOR_PROCESSING_S_M03 = 7


@allure.epic("Биллинг")
@allure.feature("Тариф 'Помесячный'")
@allure.story("E2E-MONTHLY-03: Частичное списание из пакета, остаток деньгами (внешняя сеть)")
@allure.title("Проверка списания из пакета и деньгами (ТП 'Помесячный', звонок на внешнюю сеть)")
@allure.description("""
    Тест проверяет сценарий, когда абонент с 'Помесячным' тарифом совершает исходящий звонок
    на номер ВНЕШНЕГО оператора, и длительность звонка превышает остаток минут в пакете.
    Ожидаемое поведение:
    1. Имеющиеся пакетные минуты списываются полностью.
    2. Оставшаяся часть длительности звонка тарифицируется с денежного баланса
       по тарифу для звонков на внешние сети сверх пакета.
    Шаги:
    1. Создание/обновление данных звонящего абонента (P2, Помесячный).
    2. Отправка CDR о звонке на внешний номер в RabbitMQ.
    3. Расчет общей длительности, минут из пакета, минут сверх пакета и стоимости минут сверх пакета (для внешних вызовов).
    4. Проверка полного списания пакетных минут у звонящего (P2).
    5. Проверка корректного списания средств с денежного баланса звонящего (P2).
""")
@allure.severity(allure.severity_level.CRITICAL)
def test_e2e_monthly_03_partial_package_and_external_billing(db_connection: psycopg.Connection):
    """
    E2E-MONTHLY-03: ТП Помесячный, исходящий звонок на внешнюю сеть.
    Частичное списание из пакета, остаток - деньгами по тарифу для внешней сети.
    """
    with allure.step("1. Подготовка данных звонящего абонента P2 (Помесячный)"):
        subscriber_p2_data = SubscriberCreationData(
            msisdn=MSISDN_P2_M03,
            money=INITIAL_BALANCE_P2_M03,
            tariff_id_logical=TARIFF_ID_P2_M03,
            name_prefix="P2_Monthly03_",
            quant_s_type_id=SERVICE_TYPE_ID_P2_PACKAGE_M03,
            quant_amount_left=INITIAL_PACKAGE_MINUTES_P2_M03
        )
        allure.attach(subscriber_p2_data.model_dump_json(indent=2), name="Данные абонента P2 (вход)",
                      attachment_type=allure.attachment_type.JSON)

        subscribers_info = create_or_update_subscribers_with_related_data(
            db_connection, [subscriber_p2_data]  # Создаем только P2, т.к. P1 - внешний
        )

        p2_id = subscribers_info.get(MSISDN_P2_M03)
        assert p2_id is not None, f"Не удалось получить ID для абонента {MSISDN_P2_M03}"
        allure.attach(f"ID абонента P2: {p2_id}", name="ID созданного/обновленного абонента P2",
                      attachment_type=allure.attachment_type.TEXT)

    with allure.step(
            f"2. Отправка CDR: {MSISDN_P2_M03} -> {MSISDN_EXTERNAL_CALLEE_M03} (внешний), Начало: {CDR_CALL_START_M03}, Конец: {CDR_CALL_END_M03}"):
        cdr_to_send = [{
            "callType": CDR_CALL_TYPE_M03,
            "firstSubscriberMsisdn": MSISDN_P2_M03,
            "secondSubscriberMsisdn": MSISDN_EXTERNAL_CALLEE_M03,  # Звонок на внешний номер
            "callStart": CDR_CALL_START_M03,
            "callEnd": CDR_CALL_END_M03
        }]
        allure.attach(str(cdr_to_send), name="Отправляемый CDR (на внешнюю сеть)",
                      attachment_type=allure.attachment_type.JSON)
        assert send_cdr_list_to_rabbitmq(cdr_to_send), "Ошибка отправки CDR в RabbitMQ"

    with allure.step(
            "3. Расчеты: общая длительность, минуты из пакета, минуты сверх пакета, стоимость (внешний тариф)"):
        call_duration_total_minutes = calculate_billed_minutes(CDR_CALL_START_M03, CDR_CALL_END_M03)
        allure.attach(f"Общая биллинговая длительность звонка: {call_duration_total_minutes} мин",
                      name="Общая длительность", attachment_type=allure.attachment_type.TEXT)
        assert call_duration_total_minutes == 5, \
            f"Расчетная общая длительность звонка ({call_duration_total_minutes} мин) не равна 5."

        minutes_from_package_used = min(INITIAL_PACKAGE_MINUTES_P2_M03, call_duration_total_minutes)
        allure.attach(f"Использовано минут из пакета: {minutes_from_package_used}", name="Минуты из пакета",
                      attachment_type=allure.attachment_type.TEXT)

        minutes_billed_from_money = call_duration_total_minutes - minutes_from_package_used
        allure.attach(f"Минут сверх пакета (тарифицируются деньгами): {minutes_billed_from_money}",
                      name="Минуты сверх пакета", attachment_type=allure.attachment_type.TEXT)

        cost_for_billed_minutes = minutes_billed_from_money * COST_PER_MINUTE_OVER_PACKAGE_EXTERNAL_M03
        allure.attach(
            f"Стоимость минуты сверх пакета (внешний): {COST_PER_MINUTE_OVER_PACKAGE_EXTERNAL_M03}\n"
            f"Итоговая стоимость за минуты сверх пакета: {cost_for_billed_minutes}",
            name="Стоимость минут сверх пакета (внешний)",
            attachment_type=allure.attachment_type.TEXT
        )

    with allure.step(f"Пауза {PAUSE_FOR_PROCESSING_S_M03} сек. для обработки биллинга"):
        sleep(PAUSE_FOR_PROCESSING_S_M03)

    with allure.step(f"4. Проверка пакетных минут у звонящего абонента P2 ({MSISDN_P2_M03})"):
        expected_package_minutes_after = 0
        current_package_minutes_after = get_quant_service_balance(
            db_connection, p2_id, SERVICE_TYPE_ID_P2_PACKAGE_M03
        )
        allure.attach(
            f"Начальный пакет: {INITIAL_PACKAGE_MINUTES_P2_M03}\n"
            f"Использовано из пакета: {minutes_from_package_used}\n"
            f"Ожидаемый остаток пакета: {expected_package_minutes_after}\n"
            f"Текущий остаток пакета: {current_package_minutes_after}",
            name=f"Детали пакетных минут P2 ({MSISDN_P2_M03})",
            attachment_type=allure.attachment_type.TEXT
        )
        assert current_package_minutes_after is not None, \
            f"Не удалось получить остаток пакетных минут для p_id {p2_id}"
        assert current_package_minutes_after == expected_package_minutes_after, \
            f"Остаток пакетных минут: {current_package_minutes_after}, ожидалось: {expected_package_minutes_after}"

    with allure.step(f"5. Проверка денежного баланса у звонящего абонента P2 ({MSISDN_P2_M03})"):
        expected_money_balance_after_p2 = INITIAL_BALANCE_P2_M03 - cost_for_billed_minutes
        current_money_balance_after_p2 = get_sub_balance(db_connection, MSISDN_P2_M03)
        allure.attach(
            f"Начальный денежный баланс P2: {INITIAL_BALANCE_P2_M03}\n"
            f"Списано денег за минуты сверх пакета (внешний): {cost_for_billed_minutes}\n"
            f"Ожидаемый денежный баланс P2: {expected_money_balance_after_p2}\n"
            f"Текущий денежный баланс P2: {current_money_balance_after_p2}",
            name=f"Детали денежного баланса P2 ({MSISDN_P2_M03})",
            attachment_type=allure.attachment_type.TEXT
        )
        assert current_money_balance_after_p2 is not None, \
            f"Не удалось получить денежный баланс для {MSISDN_P2_M03}"
        assert current_money_balance_after_p2 == expected_money_balance_after_p2, \
            f"Денежный баланс P2: {current_money_balance_after_p2}, ожидалось: {expected_money_balance_after_p2}"

    logger.info(
        f"Тест E2E-MONTHLY-03: Абонент P2 {MSISDN_P2_M03} (ID: {p2_id})\n"
        f"  Пакет минут (s_type_id={SERVICE_TYPE_ID_P2_PACKAGE_M03}) ДО: {INITIAL_PACKAGE_MINUTES_P2_M03}, ПОСЛЕ: {current_package_minutes_after} (ожидалось: {expected_package_minutes_after})\n"
        f"  Денежный баланс P2 ДО: {INITIAL_BALANCE_P2_M03}, ПОСЛЕ: {current_money_balance_after_p2} (ожидалось: {expected_money_balance_after_p2})"
    )
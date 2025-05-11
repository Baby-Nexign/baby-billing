import logging
from time import sleep
import psycopg
import allure

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
INITIAL_BALANCE_P2_M02 = 200  # Денежный баланс P2
INITIAL_PACKAGE_MINUTES_P2_M02 = 3  # Минут в пакете у P2
TARIFF_ID_P2_M02 = 12  # "Помесячный"
SERVICE_TYPE_ID_P2_PACKAGE_M02 = 0  # ID типа услуги для пакета минут

# Абонент P1 (Классика, принимающий) - для внутрисетевого звонка
MSISDN_P1_M02_CALLEE = "79340001122"
INITIAL_BALANCE_P1_M02_CALLEE = 50
TARIFF_ID_P1_M02_CALLEE = 11

# CDR данные
CDR_CALL_TYPE_M02 = "01"  # Исходящий
CDR_CALL_START_M02 = "2025-05-01T14:00:00"
CDR_CALL_END_M02 = "2025-05-01T14:04:59"


COST_PER_MINUTE_OVER_PACKAGE_M02 = 15

PAUSE_FOR_PROCESSING_S_M02 = 7


@allure.epic("Биллинг")
@allure.feature("Тариф 'Помесячный'")
@allure.story("E2E-MONTHLY-02: Частичное списание из пакета, остаток деньгами (внутрисеть)")
@allure.title("Проверка частичного списания из пакета и деньгами (ТП 'Помесячный', внутрисеть)")
@allure.description("""
    Тест проверяет сценарий, когда абонент с 'Помесячным' тарифом совершает исходящий внутрисетевой звонок,
    длительность которого превышает остаток минут в пакете.
    Ожидаемое поведение:
    1. Имеющиеся пакетные минуты списываются полностью.
    2. Оставшаяся часть длительности звонка тарифицируется с денежного баланса по соответствующему тарифу.
    3. Баланс принимающего абонента (если он внутрисетевой) не изменяется.
    Шаги:
    1. Создание/обновление данных звонящего (P2, Помесячный) и принимающего (P1, Классика) абонентов.
    2. Отправка CDR о звонке в RabbitMQ.
    3. Расчет общей длительности звонка, минут из пакета, минут сверх пакета и стоимости минут сверх пакета.
    4. Проверка полного списания пакетных минут у звонящего (P2).
    5. Проверка корректного списания средств с денежного баланса звонящего (P2).
    6. Проверка неизменности баланса принимающего абонента (P1).
""")
@allure.severity(allure.severity_level.CRITICAL)
def test_e2e_monthly_02_partial_package_deduction_and_billing(db_connection: psycopg.Connection):
    """
    E2E-MONTHLY-02: ТП Помесячный, исходящий внутрисетевой звонок.
    Частичное списание из пакета, остаток - деньгами.
    """
    with allure.step("1. Подготовка данных абонентов (P2 - Помесячный, P1 - Классика)"):
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
        allure.attach(subscriber_p2_data.model_dump_json(indent=2), name="Данные абонента P2 (звонящий, вход)",
                      attachment_type=allure.attachment_type.JSON)
        allure.attach(subscriber_p1_data.model_dump_json(indent=2), name="Данные абонента P1 (принимающий, вход)",
                      attachment_type=allure.attachment_type.JSON)

        subscribers_info = create_or_update_subscribers_with_related_data(
            db_connection, [subscriber_p2_data, subscriber_p1_data]
        )

        p2_id = subscribers_info.get(MSISDN_P2_M02)
        assert p2_id is not None, f"Не удалось получить ID для абонента {MSISDN_P2_M02}"
        allure.attach(f"ID абонента P2: {p2_id}", name="ID созданного/обновленного абонента P2",
                      attachment_type=allure.attachment_type.TEXT)

    with allure.step(
            f"2. Отправка CDR: {MSISDN_P2_M02} -> {MSISDN_P1_M02_CALLEE}, Начало: {CDR_CALL_START_M02}, Конец: {CDR_CALL_END_M02}"):
        cdr_to_send = [{
            "callType": CDR_CALL_TYPE_M02,
            "firstSubscriberMsisdn": MSISDN_P2_M02,
            "secondSubscriberMsisdn": MSISDN_P1_M02_CALLEE,
            "callStart": CDR_CALL_START_M02,
            "callEnd": CDR_CALL_END_M02
        }]
        allure.attach(str(cdr_to_send), name="Отправляемый CDR", attachment_type=allure.attachment_type.JSON)
        assert send_cdr_list_to_rabbitmq(cdr_to_send), "Ошибка отправки CDR в RabbitMQ"

    with allure.step("3. Расчеты: общая длительность, минуты из пакета, минуты сверх пакета, стоимость сверх пакета"):
        call_duration_total_minutes = calculate_billed_minutes(CDR_CALL_START_M02, CDR_CALL_END_M02)
        allure.attach(f"Общая биллинговая длительность звонка: {call_duration_total_minutes} мин",
                      name="Общая длительность", attachment_type=allure.attachment_type.TEXT)
        assert call_duration_total_minutes == 5, \
            f"Расчетная общая длительность звонка ({call_duration_total_minutes} мин) не равна 5."

        minutes_from_package_used = min(INITIAL_PACKAGE_MINUTES_P2_M02, call_duration_total_minutes)
        allure.attach(f"Использовано минут из пакета: {minutes_from_package_used}", name="Минуты из пакета",
                      attachment_type=allure.attachment_type.TEXT)

        minutes_billed_from_money = call_duration_total_minutes - minutes_from_package_used
        allure.attach(f"Минут сверх пакета (тарифицируются деньгами): {minutes_billed_from_money}",
                      name="Минуты сверх пакета", attachment_type=allure.attachment_type.TEXT)

        cost_for_billed_minutes = minutes_billed_from_money * COST_PER_MINUTE_OVER_PACKAGE_M02
        allure.attach(
            f"Стоимость минуты сверх пакета: {COST_PER_MINUTE_OVER_PACKAGE_M02}\n"
            f"Итоговая стоимость за минуты сверх пакета: {cost_for_billed_minutes}",
            name="Стоимость минут сверх пакета",
            attachment_type=allure.attachment_type.TEXT
        )

    with allure.step(f"Пауза {PAUSE_FOR_PROCESSING_S_M02} сек. для обработки биллинга"):
        sleep(PAUSE_FOR_PROCESSING_S_M02)

    with allure.step(f"4. Проверка пакетных минут у звонящего абонента P2 ({MSISDN_P2_M02})"):
        expected_package_minutes_after = 0
        current_package_minutes_after = get_quant_service_balance(
            db_connection, p2_id, SERVICE_TYPE_ID_P2_PACKAGE_M02
        )
        allure.attach(
            f"Начальный пакет: {INITIAL_PACKAGE_MINUTES_P2_M02}\n"
            f"Использовано из пакета: {minutes_from_package_used}\n"
            f"Ожидаемый остаток пакета: {expected_package_minutes_after}\n"
            f"Текущий остаток пакета: {current_package_minutes_after}",
            name=f"Детали пакетных минут P2 ({MSISDN_P2_M02})",
            attachment_type=allure.attachment_type.TEXT
        )
        assert current_package_minutes_after is not None, \
            f"Не удалось получить остаток пакетных минут для p_id {p2_id}"
        assert current_package_minutes_after == expected_package_minutes_after, \
            f"Остаток пакетных минут: {current_package_minutes_after}, ожидалось: {expected_package_minutes_after}"

    with allure.step(f"5. Проверка денежного баланса у звонящего абонента P2 ({MSISDN_P2_M02})"):
        expected_money_balance_after_p2 = INITIAL_BALANCE_P2_M02 - cost_for_billed_minutes
        current_money_balance_after_p2 = get_sub_balance(db_connection, MSISDN_P2_M02)
        allure.attach(
            f"Начальный денежный баланс P2: {INITIAL_BALANCE_P2_M02}\n"
            f"Списано денег за минуты сверх пакета: {cost_for_billed_minutes}\n"
            f"Ожидаемый денежный баланс P2: {expected_money_balance_after_p2}\n"
            f"Текущий денежный баланс P2: {current_money_balance_after_p2}",
            name=f"Детали денежного баланса P2 ({MSISDN_P2_M02})",
            attachment_type=allure.attachment_type.TEXT
        )
        assert current_money_balance_after_p2 is not None, \
            f"Не удалось получить денежный баланс для {MSISDN_P2_M02}"
        assert current_money_balance_after_p2 == expected_money_balance_after_p2, \
            f"Денежный баланс P2: {current_money_balance_after_p2}, ожидалось: {expected_money_balance_after_p2}"

    with allure.step(f"6. Проверка баланса принимающего абонента P1 ({MSISDN_P1_M02_CALLEE}) - не должен измениться"):
        # Баланс P1 (Классика, принимающий) не должен измениться
        current_money_balance_after_p1 = get_sub_balance(db_connection, MSISDN_P1_M02_CALLEE)
        allure.attach(
            f"Начальный баланс P1: {INITIAL_BALANCE_P1_M02_CALLEE}\n"
            f"Ожидаемый баланс P1: {INITIAL_BALANCE_P1_M02_CALLEE}\n"
            f"Текущий баланс P1: {current_money_balance_after_p1}",
            name=f"Детали баланса P1 ({MSISDN_P1_M02_CALLEE})",
            attachment_type=allure.attachment_type.TEXT
        )
        assert current_money_balance_after_p1 == INITIAL_BALANCE_P1_M02_CALLEE, \
            f"Баланс P1 ({MSISDN_P1_M02_CALLEE}) изменился: {current_money_balance_after_p1}, хотя не должен был."

    logger.info(
        f"Тест E2E-MONTHLY-02: Абонент P2 {MSISDN_P2_M02} (ID: {p2_id})\n"
        f"  Пакет минут (s_type_id={SERVICE_TYPE_ID_P2_PACKAGE_M02}) ДО: {INITIAL_PACKAGE_MINUTES_P2_M02}, ПОСЛЕ: {current_package_minutes_after} (ожидалось: {expected_package_minutes_after})\n"
        f"  Денежный баланс P2 ДО: {INITIAL_BALANCE_P2_M02}, ПОСЛЕ: {current_money_balance_after_p2} (ожидалось: {expected_money_balance_after_p2})\n"
        f"  Баланс P1 ({MSISDN_P1_M02_CALLEE}) остался: {current_money_balance_after_p1} (ожидалось: {INITIAL_BALANCE_P1_M02_CALLEE})"
    )
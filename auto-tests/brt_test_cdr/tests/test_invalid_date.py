import time
import logging
import psycopg
import allure # Добавлено
import json   # Добавлено

from rabbitmq_sender import send_cdr_list_to_rabbitmq
from db_checker import get_cdr_records, prepare_database_for_cdr_test
from cdr_reader import read_cdr_file

logger = logging.getLogger(__name__)

INVALID_DATES_FILE = "test_data/invalid_dates.jsonl"
FIRST_MSISDNS_IN_INVALID_DATES_FILE = [
    "79123456789", "79996667755", "79334455667"
]

@allure.epic("Обработка CDR в BRT")
@allure.feature("Негативные сценарии - Валидация данных")
@allure.story("Отклонение CDR с невалидными датами")
@allure.title("Тест: BRT отклоняет CDR с невалидными форматами или значениями дат")
@allure.description(
    f"Проверяет, что BRT-сервис НЕ сохраняет CDR в 'cdr_record', если CDR имеет невалидный формат или значение "
    f"для 'callStart' или 'callEnd'. Используется файл: {INVALID_DATES_FILE}. Ожидается 0 записей в БД."
)
@allure.severity(allure.severity_level.CRITICAL)
def test_brt_rejects_cdr_with_invalid_date_formats(
    db_connection: psycopg.Connection
):
    """
    Проверяет, что BRT-сервис НЕ сохраняет CDR в 'cdr_record',
    если CDR имеет невалидный формат или значение для 'callStart' или 'callEnd'.
    Ожидается 0 записей в БД.
    """
    test_file_path = INVALID_DATES_FILE
    logger.info(f"--- Тест: BRT должен отбросить все CDR из {test_file_path} (невалидные даты) ---")

    with allure.step(f"1. Подготовка БД: создание/проверка абонентов {FIRST_MSISDNS_IN_INVALID_DATES_FILE} и очистка cdr_record"):
        msisdns_to_prepare = list(FIRST_MSISDNS_IN_INVALID_DATES_FILE)
        logger.info(f"Подготовка БД: создание абонентов {msisdns_to_prepare} и очистка cdr_record...")
        subscriber_ids = prepare_database_for_cdr_test(db_connection, msisdns_to_prepare)
        allure.attach(
            json.dumps(subscriber_ids, indent=2, ensure_ascii=False),
            name="ID подготовленных/проверенных абонентов",
            attachment_type=allure.attachment_type.JSON
        )
        logger.info(f"БД подготовлена. ID абонентов: {subscriber_ids}")

    with allure.step(f"2. Чтение CDR с невалидными датами из файла: {test_file_path} и проверка консистентности тестовых данных"):
        logger.info(f"Чтение CDR из файла: {test_file_path}")
        cdr_list_to_send = read_cdr_file(test_file_path)
        allure.attach(
            json.dumps(cdr_list_to_send, indent=2, ensure_ascii=False),
            name=f"Содержимое файла {test_file_path} (с невалидными датами)",
            attachment_type=allure.attachment_type.JSON
        )
        assert cdr_list_to_send, f"Тестовый файл {test_file_path} пуст или не найден."

        with allure.step("2.1 Проверка, что firstSubscriberMsisdn в тестовом файле соответствует подготовленным в БД"):
            for cdr_idx, cdr_item in enumerate(cdr_list_to_send):
                assert cdr_item.get("firstSubscriberMsisdn") in FIRST_MSISDNS_IN_INVALID_DATES_FILE, \
                    (f"Ошибка конфигурации теста: CDR #{cdr_idx+1} в файле '{test_file_path}' "
                     f"имеет firstSubscriberMsisdn '{cdr_item.get('firstSubscriberMsisdn')}', который не указан в FIRST_MSISDNS_IN_INVALID_DATES_FILE "
                     f"для подготовки в БД. Это может привести к отбраковке CDR не по причине невалидной даты.")
            allure.attach("Все firstSubscriberMsisdn в файле соответствуют подготовленным.",
                          name="Результат проверки firstSubscriberMsisdn",
                          attachment_type=allure.attachment_type.TEXT)
        logger.info(f"Прочитано {len(cdr_list_to_send)} CDR (все с потенциально невалидными датами).")

    expected_records_in_db = 0
    allure.attach(
        str(expected_records_in_db),
        name="Ожидаемое количество записей в БД после обработки",
        attachment_type=allure.attachment_type.TEXT
    )

    with allure.step(f"3. Отправка {len(cdr_list_to_send)} CDR (с невалидными датами) в RabbitMQ"):
        logger.info(f"Отправка {len(cdr_list_to_send)} CDR в RabbitMQ...")
        allure.attach(
            json.dumps(cdr_list_to_send, indent=2, ensure_ascii=False),
            name="CDRы (с невалидными датами), отправленные в RabbitMQ",
            attachment_type=allure.attachment_type.JSON
        )
        send_status = send_cdr_list_to_rabbitmq(cdr_list_to_send)
        assert send_status, "Не удалось отправить CDR в RabbitMQ."

    processing_time_seconds = 5
    with allure.step(f"4. Ожидание {processing_time_seconds} сек. для обработки CDR сервисом BRT"):
        logger.info(f"Ожидание {processing_time_seconds} сек. для обработки BRT...")
        time.sleep(processing_time_seconds)

    with allure.step(f"5. Проверка отсутствия записей в таблице cdr_record (ожидается: {expected_records_in_db})"):
        logger.info("Получение записей из cdr_record...")
        created_records = get_cdr_records(db_connection, limit=len(cdr_list_to_send) + 5)
        logger.info(f"Фактически найдено в cdr_record: {len(created_records)}.")

        allure.attach(
            f"Ожидалось записей: {expected_records_in_db}\n"
            f"Фактически найдено записей: {len(created_records)}",
            name="Сводка по записям в БД",
            attachment_type=allure.attachment_type.TEXT
        )
        if created_records:
            allure.attach(
                json.dumps(created_records, indent=2, ensure_ascii=False, default=str),
                name="Записи, полученные из cdr_record (не ожидались)",
                attachment_type=allure.attachment_type.JSON
            )
        assert len(created_records) == expected_records_in_db, \
            (f"Ожидалось {expected_records_in_db} записей в 'cdr_record' (из-за невалидных дат), "
             f"но найдено {len(created_records)}. Записи: {created_records}")

    logger.info(f"--- Тест '{test_brt_rejects_cdr_with_invalid_date_formats.__name__}' успешно завершен. ---")
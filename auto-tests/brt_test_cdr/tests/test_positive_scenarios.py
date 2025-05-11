import time
import logging
import psycopg
import allure
import json

from rabbitmq_sender import send_cdr_list_to_rabbitmq
from db_checker import get_cdr_records, prepare_database_for_cdr_test
from cdr_reader import read_cdr_file

logger = logging.getLogger(__name__)

POSITIVE_CDR_FILE = "test_data/good_cdrs.jsonl"
REQUIRED_MSISDNS_FOR_GOOD_CDR = ["79123456789", "79996667755", "79334455667"]

@allure.epic("Обработка CDR в BRT")
@allure.feature("Позитивные сценарии")
@allure.story("Успешная обработка корректного CDR файла")
@allure.title("Тест успешной обработки файла с валидными CDR записями")
@allure.description(
    "Проверяет, что корректный файл CDR (из test_data/good_cdrs.jsonl) успешно обрабатывается, "
    "и все записи из него сохраняются в базу данных cdr_record."
)
@allure.severity(allure.severity_level.CRITICAL)
def test_process_good_cdr_file_successfully(
    db_connection: psycopg.Connection
):
    """
    Проверяет, что корректный файл CDR успешно обрабатывается
    и создает количество записей в БД, равное количеству записей в файле.
    """
    logger.info(f"--- Запуск теста: Успешная обработка {POSITIVE_CDR_FILE} ---")

    with allure.step(f"1. Подготовка БД: создание/проверка абонентов {REQUIRED_MSISDNS_FOR_GOOD_CDR} и очистка cdr_record"):
        logger.info(f"Подготовка БД для абонентов: {REQUIRED_MSISDNS_FOR_GOOD_CDR}...")
        subscriber_ids = prepare_database_for_cdr_test(db_connection, REQUIRED_MSISDNS_FOR_GOOD_CDR)
        allure.attach(
            json.dumps(subscriber_ids, indent=2, ensure_ascii=False),
            name="ID подготовленных/проверенных абонентов",
            attachment_type=allure.attachment_type.JSON
        )
        assert len(subscriber_ids) == len(REQUIRED_MSISDNS_FOR_GOOD_CDR), "Не удалось подготовить БД (абоненты)"
        logger.info(f"БД подготовлена. ID абонентов: {subscriber_ids}")

    with allure.step(f"2. Чтение CDR из файла: {POSITIVE_CDR_FILE}"):
        logger.info(f"Чтение CDR из файла: {POSITIVE_CDR_FILE}")
        cdr_list_to_send = read_cdr_file(POSITIVE_CDR_FILE)
        allure.attach(
            json.dumps(cdr_list_to_send, indent=2, ensure_ascii=False),
            name=f"Содержимое файла {POSITIVE_CDR_FILE}",
            attachment_type=allure.attachment_type.JSON
        )
        assert cdr_list_to_send, f"Не удалось прочитать данные из {POSITIVE_CDR_FILE} или файл пуст"
        logger.info(f"Прочитано {len(cdr_list_to_send)} записей из файла для отправки.")

    expected_records_count = len(cdr_list_to_send)
    allure.attach(
        str(expected_records_count),
        name="Ожидаемое количество записей в БД",
        attachment_type=allure.attachment_type.TEXT
    )
    logger.info(f"Ожидаемое количество записей в БД: {expected_records_count}")

    with allure.step(f"3. Отправка {expected_records_count} CDR в RabbitMQ"):
        logger.info("Отправка CDR данных в RabbitMQ...")
        allure.attach(
            json.dumps(cdr_list_to_send, indent=2, ensure_ascii=False),
            name="CDRы, отправленные в RabbitMQ",
            attachment_type=allure.attachment_type.JSON
        )
        send_success = send_cdr_list_to_rabbitmq(cdr_list_to_send)
        assert send_success, "Не удалось отправить сообщение в RabbitMQ"

    processing_time_seconds = 7  # Настраиваемое время
    with allure.step(f"4. Ожидание {processing_time_seconds} секунд для обработки CDR сервисом BRT"):
        logger.info(f"Ожидание {processing_time_seconds} секунд для обработки CDR...")
        time.sleep(processing_time_seconds)

    with allure.step(f"5. Проверка количества записей в таблице cdr_record (ожидается: {expected_records_count})"):
        logger.info("Получение созданных записей из cdr_record...")
        created_records = get_cdr_records(db_connection, limit=expected_records_count + 5)
        logger.info(f"Фактически создано записей: {len(created_records)}")

        allure.attach(
            f"Ожидалось записей: {expected_records_count}\n"
            f"Фактически найдено записей: {len(created_records)}",
            name="Сводка по записям в БД",
            attachment_type=allure.attachment_type.TEXT
        )
        if created_records or len(created_records) != expected_records_count:
            allure.attach(
                json.dumps(created_records, indent=2, ensure_ascii=False, default=str), # default=str для datetime
                name="Записи, полученные из cdr_record",
                attachment_type=allure.attachment_type.JSON
            )
        assert len(created_records) == expected_records_count, \
            f"Ожидалось {expected_records_count} записей (согласно файлу), но создано {len(created_records)}"

    logger.info(f"--- Тест '{test_process_good_cdr_file_successfully.__name__}' УСПЕШНО завершен (проверено количество) ---")
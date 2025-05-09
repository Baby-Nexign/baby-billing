import time
import logging
import psycopg


from rabbitmq_sender import send_cdr_list_to_rabbitmq
from db_checker import get_cdr_records, prepare_database_for_cdr_test
from cdr_reader import read_cdr_file


logger = logging.getLogger(__name__)

POSITIVE_CDR_FILE = "test_data/good_cdrs.jsonl"
REQUIRED_MSISDNS_FOR_GOOD_CDR = ["79123456789", "79996667755", "79334455667"]

def test_process_good_cdr_file_successfully(
    db_connection: psycopg.Connection
):
    """
    Проверяет, что корректный файл CDR успешно обрабатывается
    и создает количество записей в БД, равное количеству записей в файле.
    """
    logger.info(f"--- Запуск теста: Успешная обработка {POSITIVE_CDR_FILE} ---")

    logger.info(f"Подготовка БД для абонентов: {REQUIRED_MSISDNS_FOR_GOOD_CDR}...")
    subscriber_ids = prepare_database_for_cdr_test(db_connection, REQUIRED_MSISDNS_FOR_GOOD_CDR)
    assert len(subscriber_ids) == len(REQUIRED_MSISDNS_FOR_GOOD_CDR), "Не удалось подготовить БД"
    logger.info(f"БД подготовлена. ID абонентов: {subscriber_ids}")

    logger.info(f"Чтение CDR из файла: {POSITIVE_CDR_FILE}")
    cdr_list_to_send = read_cdr_file(POSITIVE_CDR_FILE)
    assert cdr_list_to_send, f"Не удалось прочитать данные из {POSITIVE_CDR_FILE} или файл пуст"
    logger.info(f"Прочитано {len(cdr_list_to_send)} записей из файла для отправки.")

    expected_records_count = len(cdr_list_to_send)
    logger.info(f"Ожидаемое количество записей в БД: {expected_records_count}")


    logger.info("Отправка CDR данных в RabbitMQ...")
    send_success = send_cdr_list_to_rabbitmq(cdr_list_to_send)
    assert send_success, "Не удалось отправить сообщение в RabbitMQ"

    processing_time_seconds = 7 # Настраиваемое время
    logger.info(f"Ожидание {processing_time_seconds} секунд для обработки CDR...")
    time.sleep(processing_time_seconds)

    logger.info("Получение созданных записей из cdr_record...")
    created_records = get_cdr_records(db_connection, limit=expected_records_count + 5)

    logger.info(f"Фактически создано записей: {len(created_records)}")
    assert len(created_records) == expected_records_count, \
        f"Ожидалось {expected_records_count} записей (согласно файлу), но создано {len(created_records)}"

    logger.info(f"--- Тест '{test_process_good_cdr_file_successfully.__name__}' УСПЕШНО завершен (проверено количество) ---")
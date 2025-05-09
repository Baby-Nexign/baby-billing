import time
import logging
import psycopg

from rabbitmq_sender import send_cdr_list_to_rabbitmq
from db_checker import get_cdr_records, prepare_database_for_cdr_test
from cdr_reader import read_cdr_file

logger = logging.getLogger(__name__)

ONLY_INVALID_CALL_TYPES_FILE = "test_data/invalid_call_types.jsonl"
MSISDNS_TO_PREPARE_IN_DB = ["79123456789", "79996667755", "79334455667"]


def test_brt_rejects_cdr_with_invalid_call_type(
        db_connection: psycopg.Connection
):
    """
    Проверяет, что BRT-сервис НЕ сохраняет CDR в 'cdr_record',
    если CDR имеет невалидный 'callType'.
    Ожидается 0 записей в БД.
    """
    test_file_path = ONLY_INVALID_CALL_TYPES_FILE
    logger.info(f"--- Тест: BRT должен отбросить все CDR из {test_file_path} (невалидный callType) ---")

    logger.info(f"Подготовка БД: создание абонентов {list(MSISDNS_TO_PREPARE_IN_DB)} и очистка cdr_record...")
    prepare_database_for_cdr_test(db_connection, list(MSISDNS_TO_PREPARE_IN_DB))
    logger.info(f"БД подготовлена.")

    logger.info(f"Чтение CDR из файла: {test_file_path}")
    cdr_list_to_send = read_cdr_file(test_file_path)
    assert cdr_list_to_send, f"Тестовый файл {test_file_path} пуст или не найден."
    logger.info(f"Прочитано {len(cdr_list_to_send)} CDR (все с невалидным callType).")

    expected_records_in_db = 0  # Ожидаем, что ничего не попадет в БД

    logger.info(f"Отправка {len(cdr_list_to_send)} CDR в RabbitMQ...")
    assert send_cdr_list_to_rabbitmq(cdr_list_to_send), "Не удалось отправить CDR в RabbitMQ."

    processing_time_seconds = 5  # Время на обработку
    logger.info(f"Ожидание {processing_time_seconds} сек. для обработки BRT...")
    time.sleep(processing_time_seconds)

    logger.info("Получение записей из cdr_record...")
    created_records = get_cdr_records(db_connection, limit=len(cdr_list_to_send) + 5)
    logger.info(f"Фактически найдено в cdr_record: {len(created_records)}.")

    assert len(created_records) == expected_records_in_db, \
        (f"Ожидалось {expected_records_in_db} записей в 'cdr_record' (невалидные callType), "
         f"но найдено {len(created_records)}. Записи: {created_records}")

    logger.info(f"--- Тест '{test_brt_rejects_cdr_with_invalid_call_type.__name__}' успешно завершен. ---")

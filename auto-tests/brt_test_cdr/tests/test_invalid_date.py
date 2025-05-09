import time
import logging
import psycopg

from rabbitmq_sender import send_cdr_list_to_rabbitmq
from db_checker import get_cdr_records, prepare_database_for_cdr_test
from cdr_reader import read_cdr_file

logger = logging.getLogger(__name__)

INVALID_DATES_FILE = "test_data/invalid_dates.jsonl"
FIRST_MSISDNS_IN_INVALID_DATES_FILE = [
    "79123456789", "79996667755", "79334455667"
]


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

    msisdns_to_prepare = list(FIRST_MSISDNS_IN_INVALID_DATES_FILE)
    logger.info(f"Подготовка БД: создание абонентов {msisdns_to_prepare} и очистка cdr_record...")
    prepare_database_for_cdr_test(db_connection, msisdns_to_prepare)
    logger.info(f"БД подготовлена (только указанные firstSubscriberMsisdn).")

    logger.info(f"Чтение CDR из файла: {test_file_path}")
    cdr_list_to_send = read_cdr_file(test_file_path)
    assert cdr_list_to_send, f"Тестовый файл {test_file_path} пуст или не найден."

    for cdr_idx, cdr_item in enumerate(cdr_list_to_send):
        assert cdr_item.get("firstSubscriberMsisdn") in FIRST_MSISDNS_IN_INVALID_DATES_FILE, \
            (f"Ошибка конфигурации теста: CDR #{cdr_idx+1} в файле '{test_file_path}' "
             f"имеет firstSubscriberMsisdn '{cdr_item.get('firstSubscriberMsisdn')}', который не указан в FIRST_MSISDNS_IN_INVALID_DATES_FILE "
             f"для подготовки в БД. Это может привести к отбраковке CDR не по причине невалидной даты.")
    logger.info(f"Прочитано {len(cdr_list_to_send)} CDR (все с потенциально невалидными датами).")


    expected_records_in_db = 0

    logger.info(f"Отправка {len(cdr_list_to_send)} CDR в RabbitMQ...")
    assert send_cdr_list_to_rabbitmq(cdr_list_to_send), "Не удалось отправить CDR в RabbitMQ."

    processing_time_seconds = 5
    logger.info(f"Ожидание {processing_time_seconds} сек. для обработки BRT...")
    time.sleep(processing_time_seconds)

    logger.info("Получение записей из cdr_record...")
    created_records = get_cdr_records(db_connection, limit=len(cdr_list_to_send) + 5)
    logger.info(f"Фактически найдено в cdr_record: {len(created_records)}.")

    assert len(created_records) == expected_records_in_db, \
        (f"Ожидалось {expected_records_in_db} записей в 'cdr_record' (из-за невалидных дат), "
         f"но найдено {len(created_records)}. Записи: {created_records}")

    logger.info(f"--- Тест '{test_brt_rejects_cdr_with_invalid_date_formats.__name__}' успешно завершен. ---")
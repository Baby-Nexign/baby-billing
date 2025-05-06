import time
import logging
import psycopg
# import re # Модуль re больше не нужен для этого теста, если убираем самопроверку

from rabbitmq_sender import send_cdr_list_to_rabbitmq
from db_checker import get_cdr_records, prepare_database_for_cdr_test
from cdr_reader import read_cdr_file

logger = logging.getLogger(__name__)

INVALID_FORMAT_SECOND_MSISDN_FILE = "test_data/invalid_format_second_msisdn.jsonl"

FIRST_MSISDNS_TO_PREPARE = {
    "79123456789", "79996667755", "79334455667"
}


def test_brt_rejects_cdr_with_invalid_format_second_msisdn(
    db_connection: psycopg.Connection
):
    """
    Проверяет, что BRT-сервис НЕ сохраняет CDR в 'cdr_record',
    если 'secondSubscriberMsisdn' имеет невалидный формат.
    firstSubscriberMsisdn, callType и даты предполагаются валидными.
    Ожидается 0 записей в БД.
    Тест полагается на корректность данных в тестовом файле.
    """
    test_file_path = INVALID_FORMAT_SECOND_MSISDN_FILE
    logger.info(f"--- Тест: BRT должен отбросить CDR из {test_file_path} (невалидный формат secondSubscriberMsisdn) ---")

    # --- Arrange (Подготовка) ---
    msisdns_to_prepare_in_db = list(FIRST_MSISDNS_TO_PREPARE)
    logger.info(f"Подготовка БД: создание абонентов {msisdns_to_prepare_in_db} и очистка cdr_record...")
    prepare_database_for_cdr_test(db_connection, msisdns_to_prepare_in_db)
    logger.info(f"БД подготовлена (только указанные firstSubscriberMsisdn).")

    logger.info(f"Чтение CDR из файла: {test_file_path}")
    cdr_list_to_send = read_cdr_file(test_file_path)
    assert cdr_list_to_send, f"Тестовый файл {test_file_path} пуст или не найден."
    logger.info(f"Прочитано {len(cdr_list_to_send)} CDR для отправки (предполагается, что все с невалидным форматом secondSubscriberMsisdn).")

    expected_records_in_db = 0 # Ожидаем 0 записей в БД

    # --- Act (Действие) ---
    logger.info(f"Отправка {len(cdr_list_to_send)} CDR в RabbitMQ...")
    assert send_cdr_list_to_rabbitmq(cdr_list_to_send), "Не удалось отправить CDR в RabbitMQ."

    processing_time_seconds = 5
    logger.info(f"Ожидание {processing_time_seconds} сек. для обработки BRT...")
    time.sleep(processing_time_seconds)

    # --- Assert (Проверка) ---
    logger.info("Получение записей из cdr_record...")
    created_records = get_cdr_records(db_connection, limit=len(cdr_list_to_send) + 5)
    logger.info(f"Фактически найдено в cdr_record: {len(created_records)}.")

    assert len(created_records) == expected_records_in_db, \
        (f"Ожидалось {expected_records_in_db} записей в 'cdr_record' (из-за невалидного формата secondSubscriberMsisdn), "
         f"но найдено {len(created_records)}. Записи: {created_records}")

    logger.info(f"--- Тест '{test_brt_rejects_cdr_with_invalid_format_second_msisdn.__name__}' успешно завершен. ---")
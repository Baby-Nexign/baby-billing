# test_cdr_processing.py (или ваш файл с тестами)

import time
import logging
import psycopg

from rabbitmq_sender import send_cdr_list_to_rabbitmq
from db_checker import get_cdr_records, prepare_database_for_cdr_test
from cdr_reader import read_cdr_file

logger = logging.getLogger(__name__)

def test_no_invalid_cdr_inserted_when_end_before_start(
    db_connection: psycopg.Connection
):
    """
    Проверяет, что CDR, где время окончания ('callEnd') предшествует времени начала ('callStart'),
    НЕ добавляются в таблицу 'cdr_record'. Предполагается, что такие записи отбрасываются
    обработчиком.
    """
    cdr_file_path = "test_data/end_before_start.jsonl"
    logger.info(f"--- Запуск теста: Проверка отсутствия вставки невалидных CDR из {cdr_file_path} ---")

    required_msisdns = ["79123456789", "79996667755", "79334455667"]
    logger.info(f"Подготовка БД (очистка cdr_record, создание абонентов): {required_msisdns}...")
    subscriber_ids = prepare_database_for_cdr_test(db_connection, required_msisdns)
    assert len(subscriber_ids) == len(required_msisdns), "Не удалось подготовить абонентов в БД"
    logger.info(f"БД подготовлена. ID абонентов: {subscriber_ids}")

    logger.info(f"Чтение CDR из файла: {cdr_file_path}")
    cdr_list_to_send = read_cdr_file(cdr_file_path)
    assert cdr_list_to_send, f"Не удалось прочитать данные из {cdr_file_path} или файл пуст"
    logger.info(f"Прочитано {len(cdr_list_to_send)} записей из файла для отправки (все невалидные).")

    expected_records_in_cdr_table = 0

    logger.info("Отправка невалидных CDR данных в RabbitMQ...")
    send_success = send_cdr_list_to_rabbitmq(cdr_list_to_send)
    assert send_success, "Не удалось отправить сообщение в RabbitMQ"


    processing_time_seconds = 5
    logger.info(f"Ожидание {processing_time_seconds} секунд для обработки CDR консьюмером...")
    time.sleep(processing_time_seconds)

    logger.info("Проверка таблицы cdr_record на отсутствие новых записей...")
    records_in_cdr_table = get_cdr_records(db_connection, limit=len(cdr_list_to_send) + 5)

    assert len(records_in_cdr_table) == expected_records_in_cdr_table, \
        f"Ожидалось {expected_records_in_cdr_table} записей в 'cdr_record' после отправки невалидных данных, " \
        f"но найдено {len(records_in_cdr_table)}."

    logger.info(f"--- Тест '{test_no_invalid_cdr_inserted_when_end_before_start.__name__}' УСПЕШНО завершен: невалидные CDR не были добавлены в cdr_record. ---")
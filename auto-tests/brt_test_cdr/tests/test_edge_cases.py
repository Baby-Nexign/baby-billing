import time
import logging
import psycopg


from db_checker import get_cdr_records, prepare_database_for_cdr_test
from cdr_reader import read_cdr_file

logger = logging.getLogger(__name__)

def test_process_empty_cdr_file(
    db_connection: psycopg.Connection
):
    """
    Проверяет, что пустой файл CDR не приводит к созданию записей в БД
    и не вызывает ошибок при чтении/отправке.
    """
    cdr_file_path = "test_data/empty_cdr.jsonl"
    logger.info(f"--- Запуск теста: Обработка пустого файла {cdr_file_path} ---")

    required_msisdns: list[str] = []
    logger.info(f"Подготовка БД (очистка cdr_record)...")
    prepare_database_for_cdr_test(db_connection, required_msisdns)
    logger.info(f"БД подготовлена.")

    logger.info(f"Чтение CDR из файла: {cdr_file_path}")
    cdr_list_to_send = read_cdr_file(cdr_file_path)

    assert not cdr_list_to_send, \
        f"Ожидался пустой список из файла {cdr_file_path}, но получено: {cdr_list_to_send}"
    logger.info(f"Успешно прочитан пустой список из {cdr_file_path}.")

    logger.info("Отправка в RabbitMQ не выполняется, так как список CDR пуст.")

    # Добавим небольшую паузу на всякий случай
    processing_time_seconds = 1
    logger.info(f"Ожидание {processing_time_seconds} сек...")
    time.sleep(processing_time_seconds)

    logger.info("Получение записей из cdr_record (ожидается 0)...")
    created_records = get_cdr_records(db_connection, limit=10)

    assert len(created_records) == 0, \
        f"Ожидалось 0 записей в cdr_record после обработки пустого файла, но найдено {len(created_records)}"

    logger.info(f"--- Тест '{test_process_empty_cdr_file.__name__}' УСПЕШНО завершен ---")


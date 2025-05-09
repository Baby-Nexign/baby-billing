import time
import logging
import psycopg

from rabbitmq_sender import send_cdr_list_to_rabbitmq
from db_checker import get_cdr_records, prepare_database_for_cdr_test
from cdr_reader import read_cdr_file

logger = logging.getLogger(__name__)


# --- Тест для файла только с внешними номерами ---
def test_process_cdr_file_with_only_external_numbers(
        db_connection: psycopg.Connection
):
    """
    Проверяет, что файл CDR, содержащий только звонки между внешними
    (неизвестными BRT) номерами, не приводит к созданию записей в БД.
    """
    cdr_file_path = "test_data/all_external_cdr.jsonl"
    logger.info(f"--- Запуск теста: Обработка файла только с внешними номерами {cdr_file_path} ---")

    # --- Arrange (Подготовка) ---
    # 1. Подготавливаем БД: очищаем cdr_record и проверяем наличие "наших" абонентов,
    #    чтобы быть уверенными, что BRT их не использовал.
    standard_internal_msisdns = ["79123456789", "79996667755", "79334455667"]
    logger.info(f"Подготовка БД (очистка и проверка наличия {standard_internal_msisdns})...")
    subscriber_ids = prepare_database_for_cdr_test(db_connection, standard_internal_msisdns)
    assert len(subscriber_ids) == len(standard_internal_msisdns), "Не удалось подготовить стандартных абонентов в БД"
    logger.info(f"БД подготовлена.")

    logger.info(f"Чтение CDR из файла: {cdr_file_path}")
    cdr_list_to_send = read_cdr_file(cdr_file_path)
    assert cdr_list_to_send, f"Не удалось прочитать данные из {cdr_file_path} или файл пуст"
    assert len(cdr_list_to_send) == 10, "Ожидалось 10 записей в файле"
    logger.info(f"Прочитано {len(cdr_list_to_send)} записей из файла для отправки.")

    logger.info("Отправка CDR данных в RabbitMQ...")
    send_success = send_cdr_list_to_rabbitmq(cdr_list_to_send)
    assert send_success, "Не удалось отправить сообщение в RabbitMQ"

    processing_time_seconds = 5
    logger.info(f"Ожидание {processing_time_seconds} секунд для обработки CDR...")
    time.sleep(processing_time_seconds)

    # --- Assert (Проверка) ---
    logger.info("Получение записей из cdr_record (ожидается 0)...")
    created_records = get_cdr_records(db_connection, limit=10)

    # Проверяем, что в БД не появилось записей
    assert len(created_records) == 0, \
        f"Ожидалось 0 записей в cdr_record после обработки файла только с внешними номерами, но найдено {len(created_records)}"

    logger.info(f"--- Тест '{test_process_cdr_file_with_only_external_numbers.__name__}' УСПЕШНО завершен ---")

import time
import logging
import psycopg

from rabbitmq_sender import send_cdr_list_to_rabbitmq
from db_checker import get_cdr_records, prepare_database_for_cdr_test
from cdr_reader import read_cdr_file

logger = logging.getLogger(__name__)

COMPLEX_CDR_BATCH_FILE = "test_data/complex_cdr_batch.jsonl"

ALLOWED_FIRST_MSISDNS = {"79123456789", "79996667755", "79334455667"}


def test_brt_processes_complex_cdr_batch_minimal_checks(
        db_connection: psycopg.Connection
):
    """
    Проверяет обработку пачки из 20 CDR (10 корректных, 10 с ошибками).
    Ожидается, что только 10 корректных CDR будут сохранены.
    Тест полагается на то, что тестовый файл корректно составлен.
    """
    test_file_path = COMPLEX_CDR_BATCH_FILE
    logger.info(f"--- Тест: Комплексная обработка CDR из {test_file_path} (минимальные проверки в тесте) ---")

    msisdns_to_prepare_in_db = list(ALLOWED_FIRST_MSISDNS)
    logger.info(f"Подготовка БД: создание абонентов {msisdns_to_prepare_in_db} и очистка cdr_record...")
    prepare_database_for_cdr_test(db_connection, msisdns_to_prepare_in_db)
    logger.info(f"БД подготовлена (только разрешенные firstSubscriberMsisdn).")

    logger.info(f"Чтение CDR из файла: {test_file_path}")
    all_cdrs_from_file = read_cdr_file(test_file_path)
    # Предполагаем, что файл содержит ровно 20 записей, из которых 10 должны пройти.
    # Если это не так, тест может быть нерепрезентативным.
    assert len(all_cdrs_from_file) > 0, f"Файл {test_file_path} пуст или не найден."
    if len(all_cdrs_from_file) != 20:
        logger.warning(
            f"В файле {test_file_path} прочитано {len(all_cdrs_from_file)} CDR, ожидалось 20 для этого теста.")

    expected_records_count_in_db = 10
    logger.info(
        f"Ожидается {expected_records_count_in_db} валидных CDR для сохранения в БД (согласно структуре файла).")

    logger.info(f"Отправка {len(all_cdrs_from_file)} CDR в RabbitMQ...")
    assert send_cdr_list_to_rabbitmq(all_cdrs_from_file), "Не удалось отправить CDR в RabbitMQ."

    processing_time_seconds = 10
    logger.info(f"Ожидание {processing_time_seconds} сек. для обработки BRT...")
    time.sleep(processing_time_seconds)

    logger.info("Получение записей из cdr_record...")
    created_records = get_cdr_records(db_connection,
                                      limit=len(all_cdrs_from_file) + 5)  # Запрашиваем больше на всякий случай
    logger.info(f"Фактически найдено в cdr_record: {len(created_records)}.")

    assert len(created_records) == expected_records_count_in_db, \
        (f"Ожидалось {expected_records_count_in_db} записей в 'cdr_record' (согласно файлу), "
         f"но найдено {len(created_records)}. Записи: {created_records}")

    logger.info(f"--- Тест '{test_brt_processes_complex_cdr_batch_minimal_checks.__name__}' успешно завершен. ---")

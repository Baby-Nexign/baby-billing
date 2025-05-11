import time
import logging
import psycopg
import allure
import json

from rabbitmq_sender import send_cdr_list_to_rabbitmq
from db_checker import get_cdr_records, prepare_database_for_cdr_test
from cdr_reader import read_cdr_file

logger = logging.getLogger(__name__)

COMPLEX_CDR_BATCH_FILE = "test_data/complex_cdr_batch.jsonl"
ALLOWED_FIRST_MSISDNS = {"79123456789", "79996667755", "79334455667"}

@allure.epic("Обработка CDR в BRT")
@allure.feature("Комплексные сценарии - Смешанные данные")
@allure.story("Обработка смешанной пачки валидных и невалидных CDR")
@allure.title("Тест: BRT обрабатывает комплексную пачку CDR (валидные и невалидные)")
@allure.description(
    f"Проверяет обработку пачки CDR из файла '{COMPLEX_CDR_BATCH_FILE}', содержащей как корректные, "
    f"так и некорректные записи. Ожидается, что только корректные CDR (10 из 20 по условию теста) будут сохранены."
)
@allure.severity(allure.severity_level.CRITICAL)
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

    with allure.step(f"1. Подготовка БД: создание/проверка абонентов {list(ALLOWED_FIRST_MSISDNS)} и очистка cdr_record"):
        msisdns_to_prepare_in_db = list(ALLOWED_FIRST_MSISDNS)
        logger.info(f"Подготовка БД: создание абонентов {msisdns_to_prepare_in_db} и очистка cdr_record...")
        subscriber_ids = prepare_database_for_cdr_test(db_connection, msisdns_to_prepare_in_db)
        allure.attach(
            json.dumps(subscriber_ids, indent=2, ensure_ascii=False),
            name="ID подготовленных/проверенных абонентов",
            attachment_type=allure.attachment_type.JSON
        )
        logger.info(f"БД подготовлена (только разрешенные firstSubscriberMsisdn). ID абонентов: {subscriber_ids}")

    with allure.step(f"2. Чтение смешанной пачки CDR из файла: {test_file_path}"):
        logger.info(f"Чтение CDR из файла: {test_file_path}")
        all_cdrs_from_file = read_cdr_file(test_file_path)
        allure.attach(
            json.dumps(all_cdrs_from_file, indent=2, ensure_ascii=False),
            name=f"Содержимое файла {test_file_path} (смешанная пачка)",
            attachment_type=allure.attachment_type.JSON
        )
        assert len(all_cdrs_from_file) > 0, f"Файл {test_file_path} пуст или не найден."

        # Информация о количестве прочитанных записей (включая предупреждение, если оно есть)
        num_read_from_file = len(all_cdrs_from_file)
        expected_in_file_for_test_logic = 20 # Тест ожидает 20 для своей логики
        file_read_summary = f"Прочитано CDR из файла: {num_read_from_file}."
        if num_read_from_file != expected_in_file_for_test_logic:
            warning_message = (f"ВНИМАНИЕ: В файле {test_file_path} прочитано {num_read_from_file} CDR, "
                               f"хотя тест рассчитан на {expected_in_file_for_test_logic} записей "
                               f"(10 валидных, 10 невалидных). Результаты могут быть нерепрезентативными.")
            logger.warning(warning_message)
            file_read_summary += f"\n{warning_message}"
        allure.attach(file_read_summary, name="Сводка по чтению файла", attachment_type=allure.attachment_type.TEXT)
        logger.info(f"Прочитано {num_read_from_file} CDR для отправки.")


    expected_records_count_in_db = 10 # Ключевое ожидание теста
    allure.attach(
        str(expected_records_count_in_db),
        name="Ожидаемое количество ВАЛИДНЫХ CDR для сохранения в БД",
        attachment_type=allure.attachment_type.TEXT
    )
    logger.info(
        f"Ожидается {expected_records_count_in_db} валидных CDR для сохранения в БД (согласно структуре файла).")

    with allure.step(f"3. Отправка {len(all_cdrs_from_file)} CDR (смешанная пачка) в RabbitMQ"):
        logger.info(f"Отправка {len(all_cdrs_from_file)} CDR в RabbitMQ...")
        allure.attach(
            json.dumps(all_cdrs_from_file, indent=2, ensure_ascii=False),
            name="Все CDRы (смешанная пачка), отправленные в RabbitMQ",
            attachment_type=allure.attachment_type.JSON
        )
        send_status = send_cdr_list_to_rabbitmq(all_cdrs_from_file)
        assert send_status, "Не удалось отправить CDR в RabbitMQ."

    processing_time_seconds = 10 # Для комплексной пачки может потребоваться больше времени
    with allure.step(f"4. Ожидание {processing_time_seconds} сек. для обработки CDR сервисом BRT"):
        logger.info(f"Ожидание {processing_time_seconds} сек. для обработки BRT...")
        time.sleep(processing_time_seconds)

    with allure.step(f"5. Проверка количества сохраненных записей в таблице cdr_record (ожидается: {expected_records_count_in_db})"):
        logger.info("Получение записей из cdr_record...")
        # Запрашиваем достаточно, чтобы увидеть все потенциально созданные записи
        created_records = get_cdr_records(db_connection, limit=len(all_cdrs_from_file) + 5)
        logger.info(f"Фактически найдено в cdr_record: {len(created_records)}.")

        allure.attach(
            f"Ожидалось записей (только валидные): {expected_records_count_in_db}\n"
            f"Фактически найдено записей: {len(created_records)}",
            name="Сводка по записям в БД",
            attachment_type=allure.attachment_type.TEXT
        )
        allure.attach(
            json.dumps(created_records, indent=2, ensure_ascii=False, default=str),
            name="Записи, полученные из cdr_record",
            attachment_type=allure.attachment_type.JSON
        )
        assert len(created_records) == expected_records_count_in_db, \
            (f"Ожидалось {expected_records_count_in_db} записей в 'cdr_record' (согласно файлу), "
             f"но найдено {len(created_records)}. Записи: {created_records}")

    logger.info(f"--- Тест '{test_brt_processes_complex_cdr_batch_minimal_checks.__name__}' успешно завершен. ---")
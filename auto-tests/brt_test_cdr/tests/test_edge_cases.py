import time
import logging
import psycopg
import allure
import json

from db_checker import get_cdr_records, prepare_database_for_cdr_test
from cdr_reader import read_cdr_file

logger = logging.getLogger(__name__)

@allure.epic("Обработка CDR в BRT")
@allure.feature("Граничные случаи (Edge Cases)")
@allure.story("Обработка пустого CDR файла")
@allure.title("Тест обработки пустого CDR файла")
@allure.description(
    "Проверяет, что пустой файл CDR (test_data/empty_cdr.jsonl) не приводит к созданию записей в БД "
    "и не вызывает ошибок при чтении/отправке."
)
@allure.severity(allure.severity_level.NORMAL)
def test_process_empty_cdr_file(
    db_connection: psycopg.Connection
):
    """
    Проверяет, что пустой файл CDR не приводит к созданию записей в БД
    и не вызывает ошибок при чтении/отправке.
    """
    cdr_file_path = "test_data/empty_cdr.jsonl"
    logger.info(f"--- Запуск теста: Обработка пустого файла {cdr_file_path} ---")

    with allure.step("1. Подготовка БД (очистка cdr_record, без создания специфичных абонентов)"):
        required_msisdns: list[str] = [] # Для этого теста не нужны конкретные абоненты
        logger.info(f"Подготовка БД (очистка cdr_record)...")
        subscriber_ids = prepare_database_for_cdr_test(db_connection, required_msisdns)
        allure.attach(
            json.dumps(subscriber_ids, indent=2, ensure_ascii=False), # Будет {}
            name="ID подготовленных/проверенных абонентов (ожидается пусто)",
            attachment_type=allure.attachment_type.JSON
        )
        logger.info(f"БД подготовлена. ID затронутых абонентов: {subscriber_ids}")

    with allure.step(f"2. Чтение CDR из пустого файла: {cdr_file_path}"):
        logger.info(f"Чтение CDR из файла: {cdr_file_path}")
        cdr_list_to_send = read_cdr_file(cdr_file_path)
        allure.attach(
            json.dumps(cdr_list_to_send, indent=2, ensure_ascii=False), # Будет []
            name=f"Содержимое файла {cdr_file_path} (ожидается пусто)",
            attachment_type=allure.attachment_type.JSON
        )
        assert not cdr_list_to_send, \
            f"Ожидался пустой список из файла {cdr_file_path}, но получено: {cdr_list_to_send}"
        logger.info(f"Успешно прочитан пустой список из {cdr_file_path}.")

    with allure.step("3. Отправка CDR в RabbitMQ (не выполняется, так как список CDR пуст)"):
        logger.info("Отправка в RabbitMQ не выполняется, так как список CDR пуст.")
        allure.attach(
            "Список CDR для отправки пуст. Отправка в RabbitMQ не производилась.",
            name="Статус отправки в RabbitMQ",
            attachment_type=allure.attachment_type.TEXT
        )

    processing_time_seconds = 1
    with allure.step(f"4. Ожидание {processing_time_seconds} сек. (на случай фоновой обработки)"):
        logger.info(f"Ожидание {processing_time_seconds} сек...")
        time.sleep(processing_time_seconds)

    with allure.step("5. Проверка отсутствия записей в таблице cdr_record (ожидается: 0)"):
        expected_records_count = 0
        logger.info(f"Получение записей из cdr_record (ожидается {expected_records_count})...")
        created_records = get_cdr_records(db_connection, limit=10) # limit=10 для проверки, что ничего лишнего нет
        logger.info(f"Фактически найдено записей: {len(created_records)}.")

        allure.attach(
            f"Ожидалось записей: {expected_records_count}\n"
            f"Фактически найдено записей: {len(created_records)}",
            name="Сводка по записям в БД",
            attachment_type=allure.attachment_type.TEXT
        )
        if created_records: # Прикрепляем только если что-то неожиданно нашлось
            allure.attach(
                json.dumps(created_records, indent=2, ensure_ascii=False, default=str),
                name="Записи, полученные из cdr_record (не ожидались)",
                attachment_type=allure.attachment_type.JSON
            )
        assert len(created_records) == expected_records_count, \
            f"Ожидалось {expected_records_count} записей в cdr_record после обработки пустого файла, но найдено {len(created_records)}"

    logger.info(f"--- Тест '{test_process_empty_cdr_file.__name__}' УСПЕШНО завершен ---")
import time
import logging
import psycopg
import allure # Добавлено
import json   # Добавлено

from rabbitmq_sender import send_cdr_list_to_rabbitmq
from db_checker import get_cdr_records, prepare_database_for_cdr_test
from cdr_reader import read_cdr_file

logger = logging.getLogger(__name__)

@allure.epic("Обработка CDR в BRT")
@allure.feature("Бизнес-логика - Обработка вызовов")
@allure.story("Игнорирование CDR между исключительно внешними номерами")
@allure.title("Тест: BRT не сохраняет CDR, если оба абонента внешние")
@allure.description(
    "Проверяет, что файл CDR, содержащий только звонки между внешними (неизвестными BRT) номерами, "
    "не приводит к созданию записей в БД. Используется файл: test_data/all_external_cdr.jsonl."
)
@allure.severity(allure.severity_level.NORMAL)
def test_process_cdr_file_with_only_external_numbers(
        db_connection: psycopg.Connection
):
    """
    Проверяет, что файл CDR, содержащий только звонки между внешними
    (неизвестными BRT) номерами, не приводит к созданию записей в БД.
    """
    cdr_file_path = "test_data/all_external_cdr.jsonl"
    logger.info(f"--- Запуск теста: Обработка файла только с внешними номерами {cdr_file_path} ---")

    standard_internal_msisdns = ["79123456789", "79996667755", "79334455667"]
    with allure.step(f"1. Подготовка БД: очистка cdr_record и проверка наличия 'внутренних' абонентов ({standard_internal_msisdns})"):
        logger.info(f"Подготовка БД (очистка и проверка наличия {standard_internal_msisdns})...")
        subscriber_ids = prepare_database_for_cdr_test(db_connection, standard_internal_msisdns)
        allure.attach(
            json.dumps(subscriber_ids, indent=2, ensure_ascii=False),
            name="ID подготовленных/проверенных 'внутренних' абонентов",
            attachment_type=allure.attachment_type.JSON
        )
        assert len(subscriber_ids) == len(standard_internal_msisdns), "Не удалось подготовить стандартных абонентов в БД"
        logger.info(f"БД подготовлена. ID 'внутренних' абонентов: {subscriber_ids}")

    with allure.step(f"2. Чтение CDR (только внешние номера) из файла: {cdr_file_path}"):
        logger.info(f"Чтение CDR из файла: {cdr_file_path}")
        cdr_list_to_send = read_cdr_file(cdr_file_path)
        allure.attach(
            json.dumps(cdr_list_to_send, indent=2, ensure_ascii=False),
            name=f"Содержимое файла {cdr_file_path} (внешние номера)",
            attachment_type=allure.attachment_type.JSON
        )
        assert cdr_list_to_send, f"Не удалось прочитать данные из {cdr_file_path} или файл пуст"
        # Тест предполагает определенное количество записей в файле
        expected_records_in_file = 10
        allure.attach(
            f"Ожидалось записей в файле: {expected_records_in_file}\n"
            f"Прочитано записей из файла: {len(cdr_list_to_send)}",
            name="Информация о прочитанном файле",
            attachment_type=allure.attachment_type.TEXT
        )
        assert len(cdr_list_to_send) == expected_records_in_file, f"Ожидалось {expected_records_in_file} записей в файле {cdr_file_path}"
        logger.info(f"Прочитано {len(cdr_list_to_send)} записей из файла для отправки.")

    expected_records_in_db = 0
    allure.attach(
        str(expected_records_in_db),
        name="Ожидаемое количество записей в БД после обработки",
        attachment_type=allure.attachment_type.TEXT
    )

    with allure.step(f"3. Отправка {len(cdr_list_to_send)} CDR (внешние номера) в RabbitMQ"):
        logger.info("Отправка CDR данных в RabbitMQ...")
        allure.attach(
            json.dumps(cdr_list_to_send, indent=2, ensure_ascii=False),
            name="CDRы (внешние номера), отправленные в RabbitMQ",
            attachment_type=allure.attachment_type.JSON
        )
        send_status = send_cdr_list_to_rabbitmq(cdr_list_to_send)
        assert send_status, "Не удалось отправить сообщение в RabbitMQ"

    processing_time_seconds = 5
    with allure.step(f"4. Ожидание {processing_time_seconds} секунд для обработки CDR сервисом BRT"):
        logger.info(f"Ожидание {processing_time_seconds} секунд для обработки CDR...")
        time.sleep(processing_time_seconds)

    with allure.step(f"5. Проверка отсутствия записей в таблице cdr_record (ожидается: {expected_records_in_db})"):
        logger.info("Получение записей из cdr_record (ожидается 0)...")
        # limit можно сделать консистентным, например len(cdr_list_to_send) + 5 или просто небольшое число
        created_records = get_cdr_records(db_connection, limit=len(cdr_list_to_send) + 5)
        logger.info(f"Фактически найдено в cdr_record: {len(created_records)}.")

        allure.attach(
            f"Ожидалось записей: {expected_records_in_db}\n"
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
        assert len(created_records) == expected_records_in_db, \
            f"Ожидалось {expected_records_in_db} записей в cdr_record после обработки файла только с внешними номерами, но найдено {len(created_records)}"

    logger.info(f"--- Тест '{test_process_cdr_file_with_only_external_numbers.__name__}' УСПЕШНО завершен ---")
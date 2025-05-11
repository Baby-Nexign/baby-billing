import time
import logging
import psycopg
import allure
import json

from rabbitmq_sender import send_cdr_list_to_rabbitmq
from db_checker import get_cdr_records, prepare_database_for_cdr_test
from cdr_reader import read_cdr_file

logger = logging.getLogger(__name__)

@allure.epic("Обработка CDR в BRT")
@allure.feature("Негативные сценарии - Логическая валидация")
@allure.story("Отклонение CDR, где callEnd раньше callStart")
@allure.title("Тест: BRT отклоняет CDR, если время окончания звонка раньше времени начала")
@allure.description(
    "Проверяет, что CDR, где время окончания ('callEnd') предшествует времени начала ('callStart'), "
    "НЕ добавляются в таблицу 'cdr_record'. Используется файл: test_data/end_before_start.jsonl. "
    "Ожидается 0 записей в БД."
)
@allure.severity(allure.severity_level.CRITICAL)
def test_no_invalid_cdr_inserted_when_end_before_start(
    db_connection: psycopg.Connection
):
    """
    Проверяет, что CDR, где время окончания ('callEnd') предшествует времени начала ('callStart'),
    НЕ добавляются в таблицу 'cdr_record'. Предполагается, что такие записи отбрасываются
    обработчиком.
    """
    cdr_file_path = "test_data/end_before_start.jsonl"
    logger.info(f"--- Запуск теста: Проверка отсутствия вставки невалидных CDR (callEnd < callStart) из {cdr_file_path} ---")

    required_msisdns = ["79123456789", "79996667755", "79334455667"]
    with allure.step(f"1. Подготовка БД: создание/проверка абонентов {required_msisdns} и очистка cdr_record"):
        logger.info(f"Подготовка БД (очистка cdr_record, создание абонентов): {required_msisdns}...")
        subscriber_ids = prepare_database_for_cdr_test(db_connection, required_msisdns)
        allure.attach(
            json.dumps(subscriber_ids, indent=2, ensure_ascii=False),
            name="ID подготовленных/проверенных абонентов",
            attachment_type=allure.attachment_type.JSON
        )
        assert len(subscriber_ids) == len(required_msisdns), "Не удалось подготовить абонентов в БД"
        logger.info(f"БД подготовлена. ID абонентов: {subscriber_ids}")

    with allure.step(f"2. Чтение CDR (callEnd < callStart) из файла: {cdr_file_path}"):
        logger.info(f"Чтение CDR из файла: {cdr_file_path}")
        cdr_list_to_send = read_cdr_file(cdr_file_path)
        allure.attach(
            json.dumps(cdr_list_to_send, indent=2, ensure_ascii=False),
            name=f"Содержимое файла {cdr_file_path} (callEnd < callStart)",
            attachment_type=allure.attachment_type.JSON
        )
        assert cdr_list_to_send, f"Не удалось прочитать данные из {cdr_file_path} или файл пуст"
        logger.info(f"Прочитано {len(cdr_list_to_send)} записей из файла для отправки (все невалидные).")

    expected_records_in_cdr_table = 0
    allure.attach(
        str(expected_records_in_cdr_table),
        name="Ожидаемое количество записей в БД после обработки",
        attachment_type=allure.attachment_type.TEXT
    )

    with allure.step(f"3. Отправка {len(cdr_list_to_send)} CDR (callEnd < callStart) в RabbitMQ"):
        logger.info("Отправка невалидных CDR данных в RabbitMQ...")
        allure.attach(
            json.dumps(cdr_list_to_send, indent=2, ensure_ascii=False),
            name="CDRы (callEnd < callStart), отправленные в RabbitMQ",
            attachment_type=allure.attachment_type.JSON
        )
        send_success = send_cdr_list_to_rabbitmq(cdr_list_to_send)
        assert send_success, "Не удалось отправить сообщение в RabbitMQ"

    processing_time_seconds = 5
    with allure.step(f"4. Ожидание {processing_time_seconds} секунд для обработки CDR сервисом BRT"):
        logger.info(f"Ожидание {processing_time_seconds} секунд для обработки CDR консьюмером...")
        time.sleep(processing_time_seconds)

    with allure.step(f"5. Проверка отсутствия записей в таблице cdr_record (ожидается: {expected_records_in_cdr_table})"):
        logger.info("Проверка таблицы cdr_record на отсутствие новых записей...")
        # Используем переменную created_records для консистентности с другими тестами
        created_records = get_cdr_records(db_connection, limit=len(cdr_list_to_send) + 5)
        logger.info(f"Фактически найдено в cdr_record: {len(created_records)}.")

        allure.attach(
            f"Ожидалось записей: {expected_records_in_cdr_table}\n"
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
        assert len(created_records) == expected_records_in_cdr_table, \
            f"Ожидалось {expected_records_in_cdr_table} записей в 'cdr_record' после отправки невалидных данных, " \
            f"но найдено {len(created_records)}."

    logger.info(f"--- Тест '{test_no_invalid_cdr_inserted_when_end_before_start.__name__}' УСПЕШНО завершен: невалидные CDR не были добавлены в cdr_record. ---")
import logging
from datetime import datetime

import psycopg

from config import get_settings
from subscriber_schema import SubscriberCreationData

settings = get_settings()

logger = logging.getLogger(__name__)
if not logger.hasHandlers():
    logging.basicConfig(level=logging.INFO, format='%(asctime)s - %(name)s - %(levelname)s - %(message)s')


def connect_db(db_name, db_url) -> psycopg.Connection | None:
    try:
        conn = psycopg.connect(db_url, autocommit=False)
        logger.info(
            f"Успешное подключение к БД {db_name}"
        )
        return conn
    except psycopg.OperationalError as e:
        logger.error(f"Ошибка подключения к БД: {e}")
        return None
    except Exception as e:
        logger.error(f"Неожиданная ошибка при подключении к БД: {e}", exc_info=True)
        return None


def close_db(conn: psycopg.Connection | None):
    if conn and not conn.closed:
        try:
            if conn.info.transaction_status != psycopg.pq.TransactionStatus.IDLE:
                conn.rollback()
                logger.warning("Откат незавершенной транзакции при закрытии соединения.")
            conn.close()
            logger.info("Соединение с БД закрыто.")
        except Exception as e:
            logger.error(f"Ошибка при закрытии соединения с БД: {e}", exc_info=True)


def get_sub_balance(
        conn: psycopg.Connection,
        msisdn: str,
) -> float | None:
    if not conn or conn.closed:
        logger.error("Соединение с БД отсутствует или закрыто.")
        return None
    try:
        with conn.cursor() as cur:
            cur.execute(
                "SELECT money FROM person WHERE msisdn = %s;",
                (msisdn,)
            )
            result = cur.fetchone()
            if result:
                balance = result[0]
                logger.debug(f"Баланс для MSISDN {msisdn} найден: {balance}")
                return balance
            else:
                logger.warning(f"Абонент с MSISDN {msisdn} не найден в таблице person.")
                return None
    except psycopg.Error as e:
        logger.error(f"Ошибка psycopg при получении баланса для MSISDN {msisdn}: {e}", exc_info=True)
        return None
    except Exception as e:
        logger.error(f"Неожиданная ошибка при получении баланса для MSISDN {msisdn}: {e}", exc_info=True)
        return None


def get_quant_service_balance(conn: psycopg.Connection, person_id: int, s_type_id: int = 0) -> int | None:
    try:
        with conn.cursor() as cur:
            cur.execute(
                "SELECT amount_left FROM quant_services WHERE p_id = %s AND s_type_id = %s;",
                (person_id, s_type_id)
            )
            result = cur.fetchone()
            if result:
                return result[0]
            else:
                return 0
    except Exception as e:
        print(f"Ошибка при получении баланса quant_services для p_id {person_id}, s_type_id {s_type_id}: {e}")
        return None


def create_or_update_subscribers_with_related_data(  # Функция переименована
        conn: psycopg.Connection,
        subscribers_to_process: list[SubscriberCreationData]
) -> dict[str, int]:
    final_processed_ids_map: dict[str, int] = {}

    if not conn or conn.closed:
        logger.error("Соединение с БД отсутствует.")
        return {}

    all_msisdns_to_check = [data.msisdn for data in subscribers_to_process]
    existing_persons_map: dict[str, int] = {}

    try:
        with conn.cursor(row_factory=psycopg.rows.dict_row) as cur:
            if all_msisdns_to_check:
                cur.execute(
                    "SELECT id, msisdn FROM person WHERE msisdn = ANY(%s);",
                    (all_msisdns_to_check,)
                )
                for row in cur.fetchall():
                    existing_persons_map[row['msisdn']] = row['id']

                if existing_persons_map:
                    logger.info(f"Найдены существующие абоненты для MSISDNs: {list(existing_persons_map.keys())}")

            for subscriber_data in subscribers_to_process:
                msisdn = subscriber_data.msisdn
                current_timestamp = datetime.now()

                if msisdn in existing_persons_map:
                    existing_person_id = existing_persons_map[msisdn]
                    logger.info(f"Обновление данных для существующего абонента {msisdn} (ID: {existing_person_id}).")

                    person_tariff_insert_query = """
                                                 INSERT INTO person_tariff (t_id, start_date)
                                                 VALUES (%s, %s)
                                                 RETURNING id;
                                                 """
                    cur.execute(person_tariff_insert_query, (subscriber_data.tariff_id_logical, current_timestamp))
                    new_person_tariff_row = cur.fetchone()
                    if not new_person_tariff_row:
                        logger.error(
                            f"Не удалось создать запись в 'person_tariff' для обновления msisdn: {msisdn}. Транзакция будет отменена.")
                        conn.rollback()
                        return {}
                    new_person_tariff_id = new_person_tariff_row['id']
                    logger.debug(
                        f"Новая запись 'person_tariff' (id: {new_person_tariff_id}) создана для msisdn: {msisdn} при обновлении.")

                    final_name = f"{subscriber_data.name_prefix}{existing_person_id}"
                    person_update_query = """
                                          UPDATE person
                                          SET money         = %s,
                                              is_restricted = %s,
                                              description   = %s,
                                              tariff_id     = %s,
                                              name          = %s
                                          WHERE id = %s;
                                          """
                    person_update_values = (
                        subscriber_data.money,
                        subscriber_data.is_restricted,
                        subscriber_data.description,
                        new_person_tariff_id,
                        final_name,
                        existing_person_id
                    )
                    cur.execute(person_update_query, person_update_values)
                    if cur.rowcount == 0:
                        logger.warning(
                            f"Обновление 'person' для ID {existing_person_id} (msisdn: {msisdn}) не затронуло ни одной строки. Это неожиданно.")
                    logger.debug(f"Запись 'person' (id: {existing_person_id}) обновлена для msisdn: {msisdn}.")

                    qs_update_query = "UPDATE quant_services SET amount_left = %s WHERE p_id = %s AND s_type_id = %s;"
                    cur.execute(qs_update_query, (subscriber_data.quant_amount_left, existing_person_id,
                                                  subscriber_data.quant_s_type_id))

                    if cur.rowcount == 0:
                        qs_insert_query = """
                                          INSERT INTO quant_services (p_id, s_type_id, amount_left)
                                          VALUES (%s, %s, %s)
                                          RETURNING id;
                                          """
                        cur.execute(qs_insert_query, (existing_person_id, subscriber_data.quant_s_type_id,
                                                      subscriber_data.quant_amount_left))
                        inserted_quant_row = cur.fetchone()
                        if not inserted_quant_row:
                            logger.error(
                                f"Не удалось создать запись в 'quant_services' для person_id: {existing_person_id} (msisdn: {msisdn}) при обновлении. Транзакция будет отменена.")
                            conn.rollback()
                            return {}
                        logger.debug(
                            f"Запись 'quant_services' (id: {inserted_quant_row['id']}) создана для person_id: {existing_person_id} при обновлении.")
                    else:
                        logger.debug(
                            f"Запись 'quant_services' обновлена для person_id: {existing_person_id} (msisdn: {msisdn}).")

                    final_processed_ids_map[msisdn] = existing_person_id
                    logger.info(f"Успешно обновлен абонент {msisdn} (person.id: {existing_person_id}).")

                else:
                    logger.info(f"Создание нового абонента для {msisdn}.")

                    person_tariff_insert_query = """
                                                 INSERT INTO person_tariff (t_id, start_date)
                                                 VALUES (%s, %s)
                                                 RETURNING id;
                                                 """
                    cur.execute(person_tariff_insert_query, (subscriber_data.tariff_id_logical, current_timestamp))
                    inserted_person_tariff_row = cur.fetchone()
                    if not inserted_person_tariff_row:
                        logger.error(
                            f"Не удалось создать запись в 'person_tariff' для msisdn: {msisdn}. Транзакция будет отменена.")
                        conn.rollback()
                        return {}
                    new_person_tariff_id = inserted_person_tariff_row['id']
                    logger.debug(f"Запись 'person_tariff' (id: {new_person_tariff_id}) создана для msisdn: {msisdn}.")

                    person_insert_query = """
                                          INSERT INTO person (msisdn, money, is_restricted, reg_data, description, tariff_id)
                                          VALUES (%s, %s, %s, %s, %s, %s)
                                          RETURNING id;
                                          """
                    person_insert_values = (
                        msisdn,
                        subscriber_data.money,
                        subscriber_data.is_restricted,
                        current_timestamp,  # reg_data
                        subscriber_data.description,
                        new_person_tariff_id
                    )
                    cur.execute(person_insert_query, person_insert_values)
                    inserted_person_row = cur.fetchone()
                    if not inserted_person_row:
                        logger.error(
                            f"Не удалось создать запись в 'person' для msisdn: {msisdn}. Транзакция будет отменена.")
                        conn.rollback()
                        return {}
                    new_person_id = inserted_person_row['id']

                    final_name = f"{subscriber_data.name_prefix}{new_person_id}"
                    person_update_name_query = "UPDATE person SET name = %s WHERE id = %s;"
                    cur.execute(person_update_name_query, (final_name, new_person_id))
                    if cur.rowcount == 0:
                        logger.warning(
                            f"Обновление имени для только что созданного person.id {new_person_id} (msisdn: {msisdn}) не затронуло ни одной строки.")
                    logger.debug(
                        f"Абонент 'person' (id: {new_person_id}, msisdn: {msisdn}) создан, имя обновлено на '{final_name}'.")

                    quant_services_insert_query = """
                                                  INSERT INTO quant_services (p_id, s_type_id, amount_left)
                                                  VALUES (%s, %s, %s)
                                                  RETURNING id;
                                                  """
                    cur.execute(quant_services_insert_query,
                                (new_person_id, subscriber_data.quant_s_type_id, subscriber_data.quant_amount_left))
                    inserted_quant_row = cur.fetchone()
                    if not inserted_quant_row:
                        logger.error(
                            f"Не удалось создать запись в 'quant_services' для person_id: {new_person_id} (msisdn: {msisdn}). Транзакция будет отменена.")
                        conn.rollback()
                        return {}
                    logger.debug(
                        f"Запись 'quant_services' (id: {inserted_quant_row['id']}) создана для person_id: {new_person_id}.")

                    final_processed_ids_map[msisdn] = new_person_id
                    logger.info(f"Успешно создан абонент {msisdn} (person.id: {new_person_id}) и связанные записи.")

            conn.commit()
            logger.info(
                f"Транзакция успешно зафиксирована. Всего обработано абонентов: {len(subscribers_to_process)}."
            )
            return final_processed_ids_map

    except psycopg.Error as e:
        logger.error(f"Ошибка psycopg при выполнении операций с БД: {e}", exc_info=True)
        if conn and not conn.closed:
            try:
                conn.rollback()
                logger.info("Транзакция отменена из-за ошибки psycopg.")
            except Exception as roll_e:
                logger.error(f"Ошибка при попытке отката транзакции после ошибки psycopg: {roll_e}", exc_info=True)
        return {}
    except Exception as e:
        logger.error(f"Произошла непредвиденная ошибка: {e}", exc_info=True)
        if conn and not conn.closed:
            try:
                conn.rollback()
                logger.info("Транзакция отменена из-за непредвиденной ошибки.")
            except Exception as roll_e:
                logger.error(f"Ошибка при попытке отката транзакции после непредвиденной ошибки: {roll_e}",
                             exc_info=True)
        return {}

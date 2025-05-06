import psycopg
import logging
from typing import List, Dict, Any

from config import get_settings

logger = logging.getLogger(__name__)
if not logger.hasHandlers():
    logging.basicConfig(level=logging.INFO, format='%(asctime)s - %(name)s - %(levelname)s - %(message)s')


def connect_db() -> psycopg.Connection | None:
    settings = get_settings()
    try:
        conn = psycopg.connect(settings.get_db_url(),
                               autocommit=False)
        logger.info(
            f"Успешное подключение к БД {settings.test_db_name} на {settings.test_db_host}:{settings.test_db_port}"
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


def clear_cdr_records(conn: psycopg.Connection):
    try:
        with conn.cursor() as cur:
            cur.execute("DELETE FROM cdr_record;")
            cur.execute("ALTER SEQUENCE cdr_record_id_seq RESTART WITH 1;")
            conn.commit()
            logger.info("Таблица cdr_record очищена, счетчик ID сброшен.")
    except Exception as e:
        logger.error(f"Ошибка при очистке таблицы cdr_record: {e}", exc_info=True)
        conn.rollback()


def ensure_subscribers_exist(conn: psycopg.Connection, msisdns: List[str]) -> Dict[str, int]:
    subscriber_ids = {}
    default_tariff_id = 1
    default_money = 100.0

    try:
        with conn.cursor(row_factory=psycopg.rows.dict_row) as cur:
            cur.execute("SELECT id, msisdn FROM person WHERE msisdn = ANY(%s)", (msisdns,))
            existing_subscribers = {row['msisdn']: row['id'] for row in cur.fetchall()}
            subscriber_ids.update(existing_subscribers)
            logger.info(f"Найдены существующие абоненты: {existing_subscribers}")

            msisdns_to_create = [msisdn for msisdn in msisdns if msisdn not in existing_subscribers]

            if msisdns_to_create:
                logger.info(f"Абоненты для создания: {msisdns_to_create}")
                values_to_insert = [(msisdn, default_money, default_tariff_id) for msisdn in msisdns_to_create]
                cur.executemany(
                    "INSERT INTO person (msisdn, money, tariff_id) VALUES (%s, %s, %s) RETURNING id, msisdn",
                    values_to_insert
                )
                newly_created = {row['msisdn']: row['id'] for row in cur.fetchall()}
                subscriber_ids.update(newly_created)
                logger.info(f"Успешно созданы новые абоненты: {newly_created}")

            conn.commit()
            return subscriber_ids

    except Exception as e:
        logger.error(f"Ошибка при проверке/создании абонентов: {e}", exc_info=True)
        conn.rollback()
        return {}


def get_cdr_records(conn: psycopg.Connection, limit: int = 100) -> List[Dict[str, Any]]:
    records = []
    try:
        with conn.cursor(row_factory=psycopg.rows.dict_row) as cur:
            cur.execute("""
                        SELECT id,
                               msisdn_one,
                               msisdn_two,
                               type,
                               start_time,
                               in_one_network,
                               our_subscriber_id,
                               lasts
                        FROM cdr_record
                        ORDER BY id DESC
                            LIMIT %s
                        """, (limit,))
            records = cur.fetchall()
            logger.info(f"Получено {len(records)} записей из cdr_record.")
    except Exception as e:
        logger.error(f"Ошибка при получении записей из cdr_record: {e}", exc_info=True)
    return records


def prepare_database_for_cdr_test(conn: psycopg.Connection, test_msisdns: List[str]) -> Dict[str, int]:
    logger.info("--- Начало подготовки БД к тесту ---")
    clear_cdr_records(conn)
    subscriber_ids = ensure_subscribers_exist(conn, test_msisdns)
    logger.info("--- Подготовка БД к тесту завершена ---")
    return subscriber_ids

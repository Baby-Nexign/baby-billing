import pytest
import logging
import psycopg
from typing import Generator, Dict

from db_checker import connect_db, close_db

logger = logging.getLogger(__name__)

TEST_SUBSCRIBER_MSISDNS = ["79123456789", "79996667755", "79334455667"]


@pytest.fixture(scope="function")
def db_connection() -> Generator[psycopg.Connection, None, None]:
    logger.info("Фикстура db_connection: Попытка подключения к БД...")
    conn = connect_db()
    if conn is None:
        pytest.fail("Не удалось подключиться к БД для теста.")
    yield conn
    logger.info("Фикстура db_connection: Закрытие соединения с БД...")
    close_db(conn)


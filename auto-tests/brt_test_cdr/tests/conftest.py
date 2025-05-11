import os
import platform

import pytest
import logging
import psycopg
from typing import Generator

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

def pytest_sessionfinish(session):
    allure_results_dir = session.config.getoption("--alluredir")
    if allure_results_dir:
        env_file_path = os.path.join(allure_results_dir, "environment.properties")

        with open(env_file_path, "w") as f:
            f.write(f"OS.System={platform.system()}\n")
            f.write(f"OS.Release={platform.release()}\n")
            f.write(f"Python.Version={platform.python_version()}\n")



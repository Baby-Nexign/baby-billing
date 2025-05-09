import pytest

from config import get_settings
from database import connect_db


@pytest.fixture(scope="function")
def db_connection():
    settings = get_settings()
    conn = connect_db(settings.brt_db_name, settings.get_brt_db_url())
    if conn is None:
        pytest.fail(f"Не удалось подключиться к БД {settings.brt_db_name}")
    yield conn
    if conn and not conn.closed:
        conn.rollback()
        conn.close()


@pytest.fixture(scope="session", autouse=True)
def reset_sequences_after_migrations():
    print("\n[Pytest Session Setup] Попытка сброса последовательностей ID...")
    settings = get_settings()
    conn = None
    try:
        conn = connect_db(settings.brt_db_name, settings.get_brt_db_url())
        with conn.cursor() as cur:
            seq_name_pt = 'public.person_tariff_id_seq'
            cur.execute(
                f"SELECT setval('{seq_name_pt}', COALESCE((SELECT MAX(id) FROM public.person_tariff), 0) + 1, false);"
            )
            conn.commit()
        print("Не удалось подключиться к БД BRT для сброса последовательностей.")
    except Exception as e:
        print(f"Ошибка при сбросе последовательностей: {e}")
    if conn:
        conn.rollback()

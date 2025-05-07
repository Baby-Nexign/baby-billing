# cdr_utils.py

import random
import datetime
from datetime import timedelta

# import string # Пока закомментируем, если не используется напрямую в этом файле

# --- ГЛОБАЛЬНЫЕ КОНСТАНТЫ И НАСТРОЙКИ ПО УМОЛЧАНИЮ ДЛЯ МОДУЛЯ ---
MSISDN_PREFIX = "79"
MSISDN_RANDOM_DIGITS_COUNT = 9
MSISDN_TOTAL_LENGTH = len(MSISDN_PREFIX) + MSISDN_RANDOM_DIGITS_COUNT

DEFAULT_START_DATE_GENERATION = datetime.datetime(2025, 1, 1, 0, 0, 0)
DEFAULT_END_DATE_GENERATION_FOR_CALL_START = datetime.datetime(2025, 12, 31, 23, 59, 59)

DEFAULT_PROBABILITY_SECOND_IS_OWN = 0.3
DEFAULT_MIN_CALL_DURATION_SECONDS = 5
DEFAULT_MAX_CALL_DURATION_SECONDS = 5 * 60 * 60  # 5 часов
DEFAULT_TIME_INCREMENT_MIN_SECONDS = 10  # мин. задержка между началами звонков
DEFAULT_TIME_INCREMENT_MAX_SECONDS = 6000  # макс. задержка

DEFAULT_OWN_SUBSCRIBERS_LIST = tuple(
    f"79{random.randint(100000000, 999999999):09d}" for _ in range(20)  # 20 случайных номеров
)


def format_datetime_iso(dt_object: datetime.datetime) -> str:
    """Форматирует datetime объект в строку ISO 8601."""
    return dt_object.strftime("%Y-%m-%dT%H:%M:%S")


def generate_random_msisdn() -> str:
    """Генерирует случайный КОРРЕКТНЫЙ MSISDN по маске 79xxxxxxxxx."""
    random_part = "".join([str(random.randint(0, 9)) for _ in range(MSISDN_RANDOM_DIGITS_COUNT)])
    return f"{MSISDN_PREFIX}{random_part}"


def generate_base_logical_call_data(
        current_earliest_start_time: datetime.datetime,
        own_subscribers: list | tuple,  # Обязательный параметр, принимаем список или кортеж
        generation_end_date: datetime.datetime = DEFAULT_END_DATE_GENERATION_FOR_CALL_START,
        probability_second_is_own: float = DEFAULT_PROBABILITY_SECOND_IS_OWN,
        min_call_duration_seconds: int = DEFAULT_MIN_CALL_DURATION_SECONDS,
        max_call_duration_seconds: int = DEFAULT_MAX_CALL_DURATION_SECONDS,
        time_increment_min_seconds: int = DEFAULT_TIME_INCREMENT_MIN_SECONDS,
        time_increment_max_seconds: int = DEFAULT_TIME_INCREMENT_MAX_SECONDS
) -> dict | None:
    """
    Генерирует базовые данные для одного "логического" звонка с корректными значениями.
    Возвращает словарь с данными или None, если не удалось сгенерировать звонок.
    """
    if not own_subscribers:
        print(
            "Критическая ошибка: Список 'своих' абонентов (own_subscribers) не может быть пустым и должен быть передан в generate_base_logical_call_data.")
        return None

    first_msisdn = random.choice(own_subscribers)
    second_msisdn = None

    attempt_own_second_msisdn = random.random() < probability_second_is_own
    if attempt_own_second_msisdn:
        potential_own_second_list = [s for s in own_subscribers if s != first_msisdn]
        if potential_own_second_list:
            second_msisdn = random.choice(potential_own_second_list)

    if second_msisdn is None:
        while True:
            candidate_msisdn = generate_random_msisdn()
            if candidate_msisdn != first_msisdn:
                second_msisdn = candidate_msisdn
                break

    call_type = random.choice(["01", "02"])
    time_increment_seconds = random.randint(time_increment_min_seconds, time_increment_max_seconds)
    call_start_dt = current_earliest_start_time + timedelta(seconds=time_increment_seconds)

    if call_start_dt > generation_end_date:
        return None

    duration_seconds = random.randint(min_call_duration_seconds, max_call_duration_seconds)
    call_end_dt = call_start_dt + timedelta(seconds=duration_seconds)

    return {
        "callType": call_type,
        "firstSubscriberMsisdn": first_msisdn,
        "secondSubscriberMsisdn": second_msisdn,
        "callStart_dt": call_start_dt,
        "callEnd_dt": call_end_dt
    }
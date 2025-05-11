import random
import string

from cdr_utils import (
    format_datetime_iso,
    generate_random_msisdn as generate_random_msisdn_correct,  # Для единообразия
    MSISDN_TOTAL_LENGTH
)

# --- Модификатор для: GARBAGE DATE ---
DEFAULT_GARBAGE_STRING_LENGTH_MIN = 10
DEFAULT_GARBAGE_STRING_LENGTH_MAX = 30


def _generate_garbage_string(min_len=DEFAULT_GARBAGE_STRING_LENGTH_MIN,
                             max_len=DEFAULT_GARBAGE_STRING_LENGTH_MAX):
    length = random.randint(min_len, max_len)
    characters = string.ascii_letters + string.digits + "!@#$%^&*()-_+=[]{}|;:',.<>/?`~"
    return "".join(random.choice(characters) for _ in range(length))


def create_cdr_with_garbage_date(logical_call_data: dict) -> dict:
    cdr_record = {
        "callType": logical_call_data["callType"],
        "firstSubscriberMsisdn": logical_call_data["firstSubscriberMsisdn"],
        "secondSubscriberMsisdn": logical_call_data["secondSubscriberMsisdn"],
        "callStart": format_datetime_iso(logical_call_data["callStart_dt"]),
        "callEnd": format_datetime_iso(logical_call_data["callEnd_dt"])
    }
    corruption_choice = random.choice(["start", "end", "both"])
    if corruption_choice == "start" or corruption_choice == "both":
        cdr_record["callStart"] = _generate_garbage_string()
    if corruption_choice == "end" or corruption_choice == "both":
        cdr_record["callEnd"] = _generate_garbage_string()
    return cdr_record


# --- Модификатор для: START AFTER END ---
def create_cdr_with_start_after_end(logical_call_data: dict) -> dict:
    erroneous_call_start_str = format_datetime_iso(logical_call_data["callEnd_dt"])
    erroneous_call_end_str = format_datetime_iso(logical_call_data["callStart_dt"])
    return {
        "callType": logical_call_data["callType"],
        "firstSubscriberMsisdn": logical_call_data["firstSubscriberMsisdn"],
        "secondSubscriberMsisdn": logical_call_data["secondSubscriberMsisdn"],
        "callStart": erroneous_call_start_str,
        "callEnd": erroneous_call_end_str
    }


# --- Модификатор для: INVALID CALL TYPE ---
DEFAULT_INVALID_CALL_TYPES_LIST = ["03", "1", "2", "outgoing", "incoming", "", None, "INVALID_TYPE", "00", "AA"]


def create_cdr_with_invalid_call_type(logical_call_data: dict,
                                      invalid_types_list: list = DEFAULT_INVALID_CALL_TYPES_LIST) -> dict:
    invalid_call_type_value = random.choice(invalid_types_list)
    return {
        "callType": invalid_call_type_value,
        "firstSubscriberMsisdn": logical_call_data["firstSubscriberMsisdn"],
        "secondSubscriberMsisdn": logical_call_data["secondSubscriberMsisdn"],
        "callStart": format_datetime_iso(logical_call_data["callStart_dt"]),
        "callEnd": format_datetime_iso(logical_call_data["callEnd_dt"])
    }


# --- Модификатор для: INVALID MSISDN ---
DEFAULT_INVALID_MSISDN_METHODS_LIST = [
    "too_short", "too_long", "with_letters", "with_specials",
    "wrong_prefix", "empty", "null_value"
]


def _generate_invalid_msisdn_value(method: str) -> str | None:
    if method == "too_short":
        len_short = random.randint(1, MSISDN_TOTAL_LENGTH - 1)
        return "".join([str(random.randint(0, 9)) for _ in range(len_short)])
    elif method == "too_long":
        len_long = random.randint(MSISDN_TOTAL_LENGTH + 1, MSISDN_TOTAL_LENGTH + 5)
        return "".join([str(random.randint(0, 9)) for _ in range(len_long)])
    elif method == "with_letters":
        base = generate_random_msisdn_correct()
        base = (base + "0" * MSISDN_TOTAL_LENGTH)[:MSISDN_TOTAL_LENGTH]
        pos = random.randint(0, MSISDN_TOTAL_LENGTH - 1)
        char_to_insert = random.choice(string.ascii_letters)
        list_base = list(base)
        list_base[pos] = char_to_insert
        return "".join(list_base)
    elif method == "with_specials":
        base = generate_random_msisdn_correct()
        base = (base + "0" * MSISDN_TOTAL_LENGTH)[:MSISDN_TOTAL_LENGTH]
        pos = random.randint(0, MSISDN_TOTAL_LENGTH - 1)
        char_to_insert = random.choice("!@#$%^&*()_+-=[]{}|;:,.<>/?")
        list_base = list(base)
        list_base[pos] = char_to_insert
        return "".join(list_base)
    elif method == "wrong_prefix":
        wrong_prefixes = ["8912", "7800", "123", "abc", ""]
        prefix = random.choice(wrong_prefixes)
        remaining_len = max(0, MSISDN_TOTAL_LENGTH - len(prefix))
        random_part = "".join([str(random.randint(0, 9)) for _ in range(remaining_len)])
        return (prefix + random_part + "0" * MSISDN_TOTAL_LENGTH)[:MSISDN_TOTAL_LENGTH]
    elif method == "empty":
        return ""
    elif method == "null_value":
        return None
    return "UNKNOWN_INVALID_MSISDN_METHOD"


def create_cdr_with_invalid_msisdn(logical_call_data: dict,
                                   invalid_msisdn_methods: list = DEFAULT_INVALID_MSISDN_METHODS_LIST) -> dict:
    msisdn1 = logical_call_data["firstSubscriberMsisdn"]
    msisdn2 = logical_call_data["secondSubscriberMsisdn"]
    corruption_target = random.choice(["first", "second", "both"])
    chosen_method = random.choice(invalid_msisdn_methods)

    if corruption_target == "first" or corruption_target == "both":
        msisdn1 = _generate_invalid_msisdn_value(chosen_method)
        if corruption_target == "both":
            chosen_method_for_second = random.choice(invalid_msisdn_methods)
            msisdn2 = _generate_invalid_msisdn_value(chosen_method_for_second)
    elif corruption_target == "second":
        msisdn2 = _generate_invalid_msisdn_value(chosen_method)

    return {
        "callType": logical_call_data["callType"],
        "firstSubscriberMsisdn": msisdn1,
        "secondSubscriberMsisdn": msisdn2,
        "callStart": format_datetime_iso(logical_call_data["callStart_dt"]),
        "callEnd": format_datetime_iso(logical_call_data["callEnd_dt"])
    }


# --- Модификатор для: ZERO DURATION ---
def create_cdr_with_zero_duration(logical_call_data: dict) -> dict:
    erroneous_call_end_dt = logical_call_data["callStart_dt"]
    return {
        "callType": logical_call_data["callType"],
        "firstSubscriberMsisdn": logical_call_data["firstSubscriberMsisdn"],
        "secondSubscriberMsisdn": logical_call_data["secondSubscriberMsisdn"],
        "callStart": format_datetime_iso(logical_call_data["callStart_dt"]),
        "callEnd": format_datetime_iso(erroneous_call_end_dt)
    }


# --- Модификатор для: MSISDN SELF CALL ---
def create_cdr_with_self_call(logical_call_data: dict) -> dict:
    return {
        "callType": logical_call_data["callType"],
        "firstSubscriberMsisdn": logical_call_data["firstSubscriberMsisdn"],
        "secondSubscriberMsisdn": logical_call_data["firstSubscriberMsisdn"],  # Ключевое изменение
        "callStart": format_datetime_iso(logical_call_data["callStart_dt"]),
        "callEnd": format_datetime_iso(logical_call_data["callEnd_dt"])
    }



ERROR_MODIFIERS = {
    "garbage_date": create_cdr_with_garbage_date,
    "start_after_end": create_cdr_with_start_after_end,
    "invalid_call_type": create_cdr_with_invalid_call_type,
    "invalid_msisdn": create_cdr_with_invalid_msisdn,
    "zero_duration": create_cdr_with_zero_duration,
    "msisdn_self_call": create_cdr_with_self_call,
}

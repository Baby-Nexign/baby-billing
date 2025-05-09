import json
import random
import string

from cdr_error_modifiers import create_cdr_with_garbage_date
from cdr_utils import (
    format_datetime_iso,
    generate_base_logical_call_data,
    DEFAULT_START_DATE_GENERATION,
    DEFAULT_END_DATE_GENERATION_FOR_CALL_START,
    DEFAULT_OWN_SUBSCRIBERS_LIST
)

OWN_SUBSCRIBERS_LIST = DEFAULT_OWN_SUBSCRIBERS_LIST

NUMBER_OF_CDRS_TO_GENERATE = 100
OUTPUT_FILENAME = "generated_cdrs_error_garbage_date.jsonl"


def main():
    if not OWN_SUBSCRIBERS_LIST:
        print("Ошибка: Список 'своих' абонентов (OWN_SUBSCRIBERS_LIST) пуст.")
        return

    generated_error_cdr_records = []
    current_call_start_anchor = DEFAULT_START_DATE_GENERATION

    print(f"Генерация {NUMBER_OF_CDRS_TO_GENERATE} CDR записей с 'мусорными' датами в файл '{OUTPUT_FILENAME}'...")

    while len(generated_error_cdr_records) < NUMBER_OF_CDRS_TO_GENERATE:
        base_call_data = generate_base_logical_call_data(
            current_earliest_start_time=current_call_start_anchor,
            own_subscribers=OWN_SUBSCRIBERS_LIST,
            generation_end_date=DEFAULT_END_DATE_GENERATION_FOR_CALL_START
        )

        if base_call_data is None:
            print(f"Достигнут конец периода генерации.")
            break

        current_call_start_anchor = base_call_data["callStart_dt"]

        error_cdr = create_cdr_with_garbage_date(base_call_data)
        generated_error_cdr_records.append(error_cdr)

    final_records_to_write = generated_error_cdr_records[:NUMBER_OF_CDRS_TO_GENERATE]
    with open(f"out_data/{OUTPUT_FILENAME}", 'w') as f:
        for record in final_records_to_write:
            json.dump(record, f)
            f.write('\n')

    print(
        f"Генерация завершена. {len(final_records_to_write)} CDR записей с 'мусорными' датами сохранено в файл {OUTPUT_FILENAME}")


if __name__ == "__main__":
    main()

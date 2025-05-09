import json

from cdr_error_modifiers import create_cdr_with_zero_duration
from cdr_utils import (
    format_datetime_iso,
    generate_base_logical_call_data,
    DEFAULT_START_DATE_GENERATION,
    DEFAULT_END_DATE_GENERATION_FOR_CALL_START,
    DEFAULT_OWN_SUBSCRIBERS_LIST
)

OUTPUT_FILENAME = f"generated_cdrs_error_zero_duration.jsonl"

OWN_SUBSCRIBERS_LIST = DEFAULT_OWN_SUBSCRIBERS_LIST
NUMBER_OF_CDRS_TO_GENERATE = 100



def main():
    if not OWN_SUBSCRIBERS_LIST:
        return

    generated_error_cdr_records = []
    current_call_start_anchor = DEFAULT_START_DATE_GENERATION

    while len(generated_error_cdr_records) < NUMBER_OF_CDRS_TO_GENERATE:
        base_call_data = generate_base_logical_call_data(
            current_earliest_start_time=current_call_start_anchor,
            own_subscribers=OWN_SUBSCRIBERS_LIST,
            generation_end_date=DEFAULT_END_DATE_GENERATION_FOR_CALL_START
        )

        if base_call_data is None:
            break

        current_call_start_anchor = base_call_data["callStart_dt"]

        error_cdr = create_cdr_with_zero_duration(base_call_data)
        generated_error_cdr_records.append(error_cdr)

    final_records_to_write = generated_error_cdr_records[:NUMBER_OF_CDRS_TO_GENERATE]

    with open(f"out_data/{OUTPUT_FILENAME}", 'w') as f:
        for record in final_records_to_write:
            json.dump(record, f)
    print(
        f"Генерация завершена. {len(generated_error_cdr_records)} CDR записей с нулевой длительностью сохранено в файл {OUTPUT_FILENAME}")


if __name__ == "__main__":
    main()

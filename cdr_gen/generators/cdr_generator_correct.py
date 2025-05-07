import json
import datetime
from datetime import timedelta

from cdr_utils import (
    format_datetime_iso,
    generate_base_logical_call_data,
    DEFAULT_START_DATE_GENERATION,
    DEFAULT_END_DATE_GENERATION_FOR_CALL_START,
    DEFAULT_OWN_SUBSCRIBERS_LIST,
)

OWN_SUBSCRIBERS_LIST = DEFAULT_OWN_SUBSCRIBERS_LIST

# Количество CDR для генерации
NUMBER_OF_CDRS_TO_GENERATE = 100

# Имя выходного файла
OUTPUT_FILENAME = "generated_cdrs_correct.jsonl"


def main():
    if not OWN_SUBSCRIBERS_LIST:
        print("Ошибка: Список 'своих' абонентов (OWN_SUBSCRIBERS_LIST) пуст.")
        return

    generated_cdr_records = []
    current_call_start_anchor = DEFAULT_START_DATE_GENERATION # Инициализация здесь

    while len(generated_cdr_records) < NUMBER_OF_CDRS_TO_GENERATE:
        logical_call = generate_base_logical_call_data(
            current_earliest_start_time=current_call_start_anchor,
            own_subscribers=OWN_SUBSCRIBERS_LIST,
            generation_end_date=DEFAULT_END_DATE_GENERATION_FOR_CALL_START
        )

        if logical_call is None:
            print(f"Достигнут конец периода генерации")
            break

        current_call_start_anchor = logical_call["callStart_dt"]
        call_type_val = logical_call["callType"]
        first_sub_val = logical_call["firstSubscriberMsisdn"]
        second_sub_val = logical_call["secondSubscriberMsisdn"]
        call_start_obj = logical_call["callStart_dt"]
        call_end_obj = logical_call["callEnd_dt"]

        if call_start_obj.date() < call_end_obj.date():
            cdr1_end_time = datetime.datetime.combine(call_start_obj.date(), datetime.time(23, 59, 59))
            cdr1 = {
                "callType": call_type_val,
                "firstSubscriberMsisdn": first_sub_val,
                "secondSubscriberMsisdn": second_sub_val,
                "callStart": format_datetime_iso(call_start_obj),
                "callEnd": format_datetime_iso(cdr1_end_time)
            }
            generated_cdr_records.append(cdr1)

            if len(generated_cdr_records) >= NUMBER_OF_CDRS_TO_GENERATE:
                break

            cdr2_start_time = datetime.datetime.combine(call_start_obj.date() + timedelta(days=1),
                                                        datetime.time(0, 0, 0))
            cdr2 = {
                "callType": call_type_val,
                "firstSubscriberMsisdn": first_sub_val,
                "secondSubscriberMsisdn": second_sub_val,
                "callStart": format_datetime_iso(cdr2_start_time),
                "callEnd": format_datetime_iso(call_end_obj)
            }
            generated_cdr_records.append(cdr2)
        else:
            cdr = {
                "callType": call_type_val,
                "firstSubscriberMsisdn": first_sub_val,
                "secondSubscriberMsisdn": second_sub_val,
                "callStart": format_datetime_iso(call_start_obj),
                "callEnd": format_datetime_iso(call_end_obj)
            }
            generated_cdr_records.append(cdr)

    final_records_to_write = generated_cdr_records[:NUMBER_OF_CDRS_TO_GENERATE]

    with open(f"out_data/{OUTPUT_FILENAME}", 'w') as f:
        for record in final_records_to_write:
            json.dump(record, f)
            f.write('\n')

    print(f"Генерация завершена. {len(final_records_to_write)} CDR записей сохранено в файл {OUTPUT_FILENAME}")


if __name__ == "__main__":
    main()

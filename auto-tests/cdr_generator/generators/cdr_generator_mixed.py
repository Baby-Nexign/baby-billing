import json
import random
import datetime
from datetime import timedelta

from cdr_utils import (
    format_datetime_iso,
    generate_base_logical_call_data,
    DEFAULT_START_DATE_GENERATION,
    DEFAULT_END_DATE_GENERATION_FOR_CALL_START,
    DEFAULT_OWN_SUBSCRIBERS_LIST
)

from cdr_error_modifiers import ERROR_MODIFIERS

OUTPUT_FILENAME = f"generated_cdrs_mixed.jsonl"

OWN_SUBSCRIBERS_LIST = DEFAULT_OWN_SUBSCRIBERS_LIST
NUMBER_OF_CDRS_TO_GENERATE = 500

PROBABILITY_OF_ERROR = 0.5  # 50% CDR будут с ошибками

# Получаем список доступных типов ошибок из ключей словаря ERROR_MODIFIERS
AVAILABLE_ERROR_TYPES = list(ERROR_MODIFIERS.keys())


def main():
    if not OWN_SUBSCRIBERS_LIST:
        return

    all_generated_cdr_records = []
    current_call_start_anchor = DEFAULT_START_DATE_GENERATION

    while len(all_generated_cdr_records) < NUMBER_OF_CDRS_TO_GENERATE:
        base_call_data = generate_base_logical_call_data(
            current_earliest_start_time=current_call_start_anchor,
            own_subscribers=OWN_SUBSCRIBERS_LIST,
            generation_end_date=DEFAULT_END_DATE_GENERATION_FOR_CALL_START
        )

        if base_call_data is None:
            break

        current_call_start_anchor = base_call_data["callStart_dt"]

        # Решаем, будет ли эта CDR ошибочной
        is_error_cdr = random.random() < PROBABILITY_OF_ERROR and AVAILABLE_ERROR_TYPES

        if is_error_cdr:
            # Генерируем ошибочную CDR
            chosen_error_type = random.choice(AVAILABLE_ERROR_TYPES)
            error_modifier_func = ERROR_MODIFIERS[chosen_error_type]
            error_cdr = error_modifier_func(base_call_data)
            all_generated_cdr_records.append(error_cdr)


        else:
            # Генерируем корректную CDR (с возможным разделением)
            call_start_obj = base_call_data["callStart_dt"]
            call_end_obj = base_call_data["callEnd_dt"]

            if call_start_obj.date() < call_end_obj.date():  # Разделение
                cdr1_end_time = datetime.datetime.combine(call_start_obj.date(), datetime.time(23, 59, 59))
                cdr1 = {
                    "callType": base_call_data["callType"],
                    "firstSubscriberMsisdn": base_call_data["firstSubscriberMsisdn"],
                    "secondSubscriberMsisdn": base_call_data["secondSubscriberMsisdn"],
                    "callStart": format_datetime_iso(call_start_obj),
                    "callEnd": format_datetime_iso(cdr1_end_time)
                }
                all_generated_cdr_records.append(cdr1)

                if len(all_generated_cdr_records) >= NUMBER_OF_CDRS_TO_GENERATE:
                    break

                cdr2_start_time = datetime.datetime.combine(call_start_obj.date() + timedelta(days=1),
                                                            datetime.time(0, 0, 0))
                cdr2 = {
                    "callType": base_call_data["callType"],
                    "firstSubscriberMsisdn": base_call_data["firstSubscriberMsisdn"],
                    "secondSubscriberMsisdn": base_call_data["secondSubscriberMsisdn"],
                    "callStart": format_datetime_iso(cdr2_start_time),
                    "callEnd": format_datetime_iso(call_end_obj)
                }
                all_generated_cdr_records.append(cdr2)
            else:  # Без разделения
                cdr = {
                    "callType": base_call_data["callType"],
                    "firstSubscriberMsisdn": base_call_data["firstSubscriberMsisdn"],
                    "secondSubscriberMsisdn": base_call_data["secondSubscriberMsisdn"],
                    "callStart": format_datetime_iso(call_start_obj),
                    "callEnd": format_datetime_iso(call_end_obj)
                }
                all_generated_cdr_records.append(cdr)

    final_records_to_write = all_generated_cdr_records[:NUMBER_OF_CDRS_TO_GENERATE]

    with open(f"out_data/{OUTPUT_FILENAME}", 'w') as f:
        for record in final_records_to_write:
            json.dump(record, f)
            f.write('\n')
    print(f"Генерация завершена. {len(final_records_to_write)} CDR записей сохранено в файл out_data/{OUTPUT_FILENAME}")


if __name__ == "__main__":
    main()

# Генератор CDR-записей

Этот проект предназначен для генерации тестовых данных CDR (Call Detail Records) - записей о телефонных вызовах. Генератор создает как корректные CDR, так и записи с различными типами ошибок.

## Установка и запуск

Для установки проекта `baby-billing` и запуска всех необходимых сервисов обратитесь к основному файлу `README.md` в корневой директории `auto-tests`.

1.  **Перейдите в директорию генератора**:
    ```bash
    # Предполагая, что вы находитесь в корневой директории baby-billing
    cd auto-tests/cdr_generator
    ```
2.  **Запустите генерацию**:
    Для генерации всех типов CDR (или тех, что активны в `main.py`):
    ```bash
    python main.py
    ```
    Сгенерированные файлы будут сохранены в директории `out_data/`.

## Структура проекта

```
cdr_generator/
├── main.py                               # Главный запускаемый файл
├── cdr_utils.py                          # Утилиты для работы с CDR
├── cdr_error_modifiers.py                # Функции для создания ошибок в CDR
├── generators/                           # Генераторы различных типов CDR
│   ├── cdr_generator_correct.py          # Генератор корректных CDR
│   ├── cdr_generator_error_garbage_date.py   # CDR с некорректными датами
│   ├── cdr_generator_error_invalid_call_type.py # CDR с неверным типом вызова
│   ├── cdr_generator_error_invalid_msisdn.py # CDR с неверным форматом номера
│   ├── cdr_generator_error_msisdn_self_call.py # CDR с вызовом на свой номер
│   ├── cdr_generator_error_start_after_end.py # CDR где начало после окончания
│   ├── cdr_generator_error_zero_duration.py # CDR с нулевой длительностью
│   └── cdr_generator_mixed.py            # Смешанные CDR (часть корректные, часть с ошибками)
└── out_data/                             # Директория для сохранения сгенерированных файлов
```

## Формат CDR-записей

Каждая CDR-запись содержит следующие поля:

- `callType`: тип вызова (01 - исходящий, 02 - входящий)
- `firstSubscriberMsisdn`: номер первого абонента (формат: 79XXXXXXXXX)
- `secondSubscriberMsisdn`: номер второго абонента (формат: 79XXXXXXXXX)
- `callStart`: время начала вызова (ISO формат: ГГГГ-ММ-ДДThh:mm:ss)
- `callEnd`: время окончания вызова (ISO формат: ГГГГ-ММ-ДДThh:mm:ss)

## Типы генерируемых CDR

1.  **Корректные CDR** (`generated_cdrs_correct.jsonl`)
    * Все поля соответствуют требуемому формату
    * Звонки разделяются на два, если они переходят через полночь

2.  **CDR с ошибками:**
    * **Некорректные даты** (`generated_cdrs_error_garbage_date.jsonl`) - случайные строки вместо дат
    * **Неверный тип вызова** (`generated_cdrs_error_invalid_call_type.jsonl`) - значения, отличные от '01' или '02'
    * **Неверный формат номера** (`generated_cdrs_error_invalid_msisdn.jsonl`) - слишком короткий, слишком длинный или с недопустимыми символами
    * **Вызов самому себе** (`generated_cdrs_error_msisdn_self_call.jsonl`) - одинаковые номера в полях first и second
    * **Время начала после окончания** (`generated_cdrs_error_start_after_end.jsonl`) - нарушение хронологии
    * **Нулевая длительность** (`generated_cdrs_error_zero_duration.jsonl`) - одинаковое время начала и окончания

3.  **Смешанные CDR** (`generated_cdrs_mixed.jsonl`)
    * Комбинация корректных записей и записей с различными ошибками

## Настройки генерации

Можно изменить следующие параметры в файлах генераторов (`generators/*.py`):

-   `NUMBER_OF_CDRS_TO_GENERATE` - количество CDR для генерации
-   `OWN_SUBSCRIBERS_LIST` - список "своих" абонентов (для корректной генерации `firstSubscriberMsisdn`)
-   В файле `cdr_generator_mixed.py` можно настроить `PROBABILITY_OF_ERROR` - вероятность генерации ошибочной записи.
-   Другие константы, такие как `DEFAULT_START_DATE_GENERATION`, `DEFAULT_MIN_CALL_DURATION_SECONDS` и т.д., можно найти и изменить в `cdr_utils.py` или непосредственно в скриптах генераторов.

## Требования

-   Python 3.10+
-   Стандартная библиотека Python (без дополнительных внешних зависимостей для самого генератора)

## Примеры сгенерированных CDR

### Корректная запись

```json
{"callType": "01", "firstSubscriberMsisdn": "79123456789", "secondSubscriberMsisdn": "79876543210", "callStart": "2025-01-01T10:15:30", "callEnd": "2025-01-01T10:25:45"}
```

### Запись с ошибкой (неверная дата)

```json
{"callType": "02", "firstSubscriberMsisdn": "79123456789", "secondSubscriberMsisdn": "79876543210", "callStart": "2025-01-01T10:15:30", "callEnd": "j8a$dK!2#@pL"}
```

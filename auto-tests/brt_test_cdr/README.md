# Тестирование BRT-сервиса: обработка CDR

Данный репозиторий содержит набор интеграционных тестов для сервиса BRT, являющегося частью системы `baby-billing`. Основная задача тестов — проверка корректности обработки CDR (Call Detail Record) файлов, которые сервис BRT получает через RabbitMQ и сохраняет в PostgreSQL.

## Установка и первоначальная настройка

Для подготовки окружения и установки зависимостей выполните следующие шаги. Более подробное руководство в главном README файле ветки `autotests`

```bash
# 1. Клонируйте ветку autotests репозитория baby-billing (если еще не сделали)
git clone -b autotests --single-branch https://github.com/Baby-Nexign/baby-billing.git
cd baby-billing

# 2. Запустите все сервисы baby-billing с помощью Docker Compose
# (выполнять из корневой директории baby-billing)
docker compose up -d

# 3. Перейдите в директорию тестов brt_test_cdr
cd auto-tests/brt_test_cdr

# 4. Создайте и активируйте виртуальное окружение Python
python -m venv .venv
# Для Linux/MacOS:
source .venv/bin/activate
# Для Windows:
# .venv\Scripts\activate

# 5. Установите зависимости Python для тестов BRT
pip install -r requirements.txt
```

## Запуск тестов BRT
Для запуска всех тестов BRT с подробным выводом:
```bash
pytest -v
```

Для запуска тестов из конкретного файла:
```bash
pytest -v tests/test_positive_scenarios.py
```
## Интеграция с Allure Report

Для получения детализированных и наглядных отчетов о выполнении тестов данный проект интегрирован с Allure Report.

### Установка Allure

1.  **Allure Pytest Adapter**: Библиотека `allure-pytest` должна быть уже включена в файл `requirements.txt` и установлена вместе с остальными зависимостями на шаге `pip install -r requirements.txt`.
2.  **Allure Commandline**: Для генерации HTML-отчета необходим установленный Allure Commandline. Инструкции по его установке для вашей операционной системы можно найти на [официальном сайте Allure Framework](https://allurereport.org/docs/gettingstarted-installation/).

### Запуск тестов для генерации Allure-данных

Чтобы Pytest собрал данные для Allure Report, используйте ключ `--alluredir`, указав директорию для сохранения результатов (обычно `allure-results`):

```bash
# Запуск всех тестов и сохранение результатов для Allure
pytest --alluredir=allure-results

# Можно комбинировать с другими ключами, например -v для подробного вывода
pytest -v --alluredir=allure-results
```

### Генерация и просмотр отчета Allure

После того как тесты выполнены и данные сохранены в `allure-results`:

1.  **Сгенерируйте HTML-отчет**:
    ```bash
    allure generate allure-results --clean -o allure-report
    ```
    * `allure-results` – папка с результатами выполнения тестов.
    * `--clean` – удаляет предыдущий сгенерированный отчет перед созданием нового.
    * `-o allure-report` – указывает папку, в которую будет помещен сгенерированный HTML-отчет (например, `allure-report`).

2.  **Откройте сгенерированный отчет**:
    ```bash
    allure open allure-report
    ```
    Эта команда запустит локальный веб-сервер и попытается открыть отчет в вашем браузере по умолчанию.
    
## Структура тестового проекта `brt_test_cdr`

```
brt_test_cdr/
├── test_data/                    # Тестовые CDR-файлы (.jsonl) для различных сценариев
│   ├── all_external_cdr.jsonl
│   ├── complex_cdr_batch.jsonl
│   ├── empty_cdr.jsonl
│   ├── end_before_start.jsonl
│   ├── good_cdrs.jsonl
│   ├── invalid_call_types.jsonl
│   ├── invalid_dates.jsonl
│   └── invalid_format_second_msisdn.jsonl
├── tests/                        # Модули с тестами Pytest
│   ├── conftest.py               # Общие фикстуры (напр., подключение к БД)
│   ├── test_complex_cdr.py
│   ├── test_edge_cases.py
│   ├── test_external_calls.py
│   ├── test_invalid_call_type.py
│   ├── test_invalid_date.py
│   ├── test_invalid_second_msisdn.py
│   ├── test_positive_scenarios.py
│   └── test_start_after_end.py
├── .env                          # Конфигурация окружения
├── cdr_reader.py                 # Модуль для чтения и парсинга CDR-файлов
├── config.py                     # Загрузка конфигурации тестов из .env
├── db_checker.py                 # Утилиты для взаимодействия с БД PostgreSQL
├── rabbitmq_sender.py            # Модуль для отправки сообщений CDR в RabbitMQ
├── allure-results/               # (Создается при запуске pytest с --alluredir) Директория с сырыми данными Allure
├── allure-report/                # (Создается при запуске allure generate) Директория с HTML-отчетом Allure
└── requirements.txt              # Зависимости Python для тестового проекта
```

## Описание тестовых сценариев

Тесты разделены по файлам в директории `tests/` и нацелены на проверку различных аспектов обработки CDR:

### 1. Позитивные сценарии (`test_positive_scenarios.py`)
   - **`test_process_good_cdr_file_successfully`**: Проверяет, что корректно сформированный CDR-файл (из `test_data/good_cdrs.jsonl`) полностью обрабатывается, и все записи из него сохраняются в базу данных `cdr_record`. Перед тестом в БД создаются или проверяются абоненты, указанные в `REQUIRED_MSISDNS_FOR_GOOD_CDR`.

### 2. Негативные сценарии (обработка невалидных данных)
   - **Невалидный тип вызова (`test_invalid_call_type.py`)**:
     - **`test_brt_rejects_cdr_with_invalid_call_type`**: Тестирует, что CDR с невалидным значением поля `callType` (например, "03", "xyz", или отсутствующее поле) из `test_data/invalid_call_types.jsonl` не сохраняются в `cdr_record`.
   - **Невалидный MSISDN второго абонента (`test_invalid_second_msisdn.py`)**:
     - **`test_brt_rejects_cdr_with_invalid_format_second_msisdn`**: Проверяет отбраковку CDR, если `secondSubscriberMsisdn` имеет неверный формат (например, слишком короткий, содержит нецифровые символы). Используется `test_data/invalid_format_second_msisdn.jsonl`.
   - **Невалидные даты (`test_invalid_date.py`)**:
     - **`test_brt_rejects_cdr_with_invalid_date_formats`**: Гарантирует, что CDR с некорректным форматом или нелогичным значением для полей `callStart` или `callEnd` (например, "2023-13-01T10:00:00Z", "не\_дата") из `test_data/invalid_dates.jsonl` не сохраняются.
   - **Время окончания раньше времени начала (`test_start_after_end.py`)**:
     - **`test_no_invalid_cdr_inserted_when_end_before_start`**: Проверяет, что CDR, у которых `callEnd` предшествует `callStart` (файл `test_data/end_before_start.jsonl`), не добавляются в `cdr_record`.

### 3. Граничные случаи
   - **Обработка пустого CDR файла (`test_edge_cases.py`)**:
     - **`test_process_empty_cdr_file`**: Удостоверяется, что отправка пустого списка CDR (эквивалентно обработке файла `test_data/empty_cdr.jsonl`) не приводит к ошибкам и не создает никаких записей в БД.
   - **Звонки только между внешними номерами (`test_external_calls.py`)**:
     - **`test_process_cdr_file_with_only_external_numbers`**: Проверяет, что CDR-файл (`test_data/all_external_cdr.jsonl`), содержащий звонки только между абонентами, не зарегистрированными в системе BRT, не приводит к созданию записей в `cdr_record`.

### 4. Комплексные тесты (`test_complex_cdr.py`)
   - **`test_brt_processes_complex_cdr_batch_minimal_checks`**: Этот тест использует файл `test_data/complex_cdr_batch.jsonl`, содержащий смесь корректных и некорректных записей. Проверяется, что в базу данных `cdr_record` попадает только ожидаемое количество валидных CDR.

## Процесс тестирования (как это работает)

1.  **Чтение данных**: Тесты (`pytest`) используют `cdr_reader.py` для чтения и парсинга тестовых CDR-записей из файлов формата `.jsonl`, расположенных в директории `test_data/`.
2.  **Подготовка БД**: Перед выполнением тестов, которые взаимодействуют с базой данных, фикстуры из `conftest.py` и функции из `db_checker.py` могут выполнять подготовку:
    * Очистка таблицы `cdr_record` в базе данных BRT.
    * Сброс счетчиков автоинкрементных полей.
    * Проверка наличия или создание необходимых тестовых абонентов в таблице `person` для обеспечения консистентности и изоляции тестовых случаев.
3.  **Отправка CDR**: Сформированный список CDR-записей отправляется в RabbitMQ (в определенный `exchange` и `routing_key`, указанные в `.env`) с помощью модуля `rabbitmq_sender.py`.
4.  **Обработка сервисом BRT**: Запущенный сервис `brt-service` (из основного проекта `baby-billing`) прослушивает очередь RabbitMQ, забирает сообщения с CDR и обрабатывает их согласно своей бизнес-логике.
5.  **Проверка результатов**: После отправки CDR и предоставления сервису BRT некоторого времени на обработку (через `time.sleep()`), тесты используют `db_checker.py` для подключения к PostgreSQL и проверки содержимого таблицы `cdr_record`. Проверки могут включать количество созданных записей, значения определенных полей и т.д., чтобы убедиться, что обработка прошла корректно и в соответствии с ожиданиями.

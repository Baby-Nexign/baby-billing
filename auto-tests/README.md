## Итоговый отчет в Allure Report
```bash
allure open allure-report-final
```

## Обзор
Проект "baby-billing" включает в себя несколько наборов автотестов, предназначенных для проверки различных компонентов системы:
* **API-автотесты (`auto-tests/api_tests`)**:
    * Написаны на Java с использованием RestAssured, JUnit 5 и Maven.
    * Предназначены для тестирования REST API эндпоинтов всех основных сервисов системы (`Auth-Service`, `BRT-Service`, `HRS-Service`, `Commutator-Service`).
    * _Детальное описание и инструкции по запуску находятся в `auto-tests/api_tests/README.md`._
* **Генератор CDR-записей**: Находится в директории `auto-tests/cdr_generator`. Предназначен для генерации тестовых данных CDR (Call Detail Records) как корректных, так и с ошибками.
* **Тестирование BRT-сервиса (обработка CDR)**: Находится в директории `auto-tests/brt_test_cdr`. Содержит интеграционные тесты для проверки корректности обработки CDR-файлов сервисом BRT. 
* **E2E (End-to-End) тесты**: Находятся в директории `auto-tests/e2e_tests`. Предназначены для сквозного тестирования взаимодействия различных сервисов системы.

## Предварительные требования

Убедитесь, что на вашем компьютере установлены:

* **Java JDK**: Версия 17 или выше (необходима для запуска Java-сервисов и API-тестов).
* **Maven**: Для сборки Java-проектов и запуска API-тестов.
* **Python**: Версия 3.10 или выше (для Python-скриптов и тестов).
* **Docker**
* **Docker Compose**
* **Git**
## Установка и запуск

Следующие шаги описывают общий процесс установки и запуска тестов. Для каждого набора тестов (cdr_generator, brt_test_cdr, e2e_tests) шаги по переходу в директорию, созданию окружения и установке зависимостей будут схожими, но выполняться в соответствующей поддиректории `auto-tests`.

1.  **Клонируйте репозиторий `baby-billing`**:
    ```
    git clone -b autotests --single-branch https://github.com/Baby-Nexign/baby-billing.git
    cd baby-billing
    ```

3.  **Запустите сервисы `baby-billing` с помощью Docker Compose**:
    В корневой директории проекта `baby-billing` выполните команду:
    ```bash
    docker compose up -d
    ```
4.  **Настройка и запуск конкретного набора тестов**:

    Вам нужно будет перейти в директорию соответствующего набора тестов внутри `auto-tests` (например, `auto-tests/cdr_generator`, `auto-tests/brt_test_cdr` или `auto-tests/e2e_tests`).

    **Пример для `auto-tests/brt_test_cdr`** (аналогично для других):

    * **Перейдите в директорию с тестами**:
        ```bash
        cd auto-tests/brt_test_cdr
        ```

    * **Создайте и активируйте виртуальное окружение**:
        ```bash
        python -m venv .venv
        # Для Linux/MacOS:
        source .venv/bin/activate
        # Для Windows:
        # .venv\Scripts\activate
        ```
       

    * **Установите зависимости Python**:
        Внутри директории конкретного набора тестов (например, `auto-tests/brt_test_cdr`) должен находиться файл `requirements.txt`. Установите зависимости:
        ```bash
        pip install -r requirements.txt
        ```
       
        Для `cdr_generator` зависимости минимальны и могут входить в стандартную библиотеку Python.
        Для `brt_test_cdr` и `e2e_tests` файлы `requirements.txt` содержат зависимости, такие как `pytest`, `pika`, `psycopg`, `pydantic`.


    **Для API-тестов на Java (`auto-tests/api_tests`):**
    
    * **Перейдите в директорию с тестами**:
      
        ```bash
        cd auto-tests/api_tests
        ```
    * **Запустите тесты** с помощью Maven:
      
        ```bash
        mvn clean test
        ```

## Структура директории `auto-tests`

```
baby-billing/
├── auto-tests/
│   ├── api_tests/              # API-тесты на Java + RestAssured
│   │   ├── src/main/java/      
│   │   ├── src/test/java/      # Исходный код API-тестов
│   │   ├── pom.xml             # Maven конфигурация
│   │   └── README.md           # README для API-тестов
├── auto-tests/
│   ├── cdr_generator/          # Генератор CDR-файлов
│   │   ├── generators/         # Скрипты для генерации различных типов CDR
│   │   ├── out_data/           # Директория для сохранения сгенерированных файлов
│   │   ├── cdr_utils.py
│   │   ├── cdr_error_modifiers.py
│   │   ├── main.py
│   │   └── README.md
│   ├── brt_test_cdr/           # Интеграционные тесты для BRT-сервиса
│   │   ├── test_data/          # Тестовые CDR-файлы для BRT
│   │   ├── tests/              # Модули с тестами Pytest для BRT
│   │   ├── .env                # Конфигурация окружения для BRT тестов
│   │   ├── cdr_reader.py
│   │   ├── config.py
│   │   ├── db_checker.py
│   │   ├── rabbitmq_sender.py
│   │   ├── requirements.txt
│   │   └── README.md
│   └── e2e_tests/              # End-to-End тесты
│       ├── tests/              # Модули с E2E тестами Pytest
│       ├── .env                # Конфигурация окружения для E2E тестов
│       ├── config.py
│       ├── database.py
│       ├── rabbitmq_sender.py
│       ├── requirements.txt
│       ├── subscriber_schema.py
│       └── utils.py
├── docker-compose.yml
└── ... (другие директории и файлы проекта)
```


## Запуск тестов

Убедитесь, что все сервисы `baby-billing` запущены через `docker compose up -d`.

* **Для `cdr_generator`**:
    Перейдите в директорию `auto-tests/cdr_generator` и выполните:
    ```bash
    python main.py
    ```
    Сгенерированные CDR-файлы будут сохранены в `auto-tests/cdr_generator/out_data/`.

* **Для `brt_test_cdr` и `e2e_tests` (используют Pytest)**:
    1.  Перейдите в соответствующую директорию (`auto-tests/brt_test_cdr` или `auto-tests/e2e_tests`).
    2.  Убедитесь, что виртуальное окружение активировано и зависимости установлены.
    3.  Для запуска всех тестов в данной директории с подробным выводом:
        ```bash
        pytest -v
        ```
    4.  Для запуска тестов из конкретного файла (например, `test_positive_scenarios.py` из `brt_test_cdr` или `test_e2e_classic_01.py` из `e2e_tests`):
        ```bash
        # Пример для brt_test_cdr
        pytest -v tests/test_positive_scenarios.py

        # Пример для e2e_tests
        pytest -v tests/test_e2e_classic_01.py
        ```

## Описание тестовых сценариев
* **`api_tests`**: Тестирование REST API эндпоинтов, включая проверку авторизации, прав доступа, корректности обработки запросов и ответов для сервисов Auth, BRT, HRS и Commutator. Подробности в `auto-tests/api_tests/README.md`.
* **`cdr_generator`**: Генерирует различные наборы CDR-данных, включая корректные записи, записи с ошибками в датах, типах вызовов, форматах номеров, нулевой длительностью и вызовами на собственный номер.
* **`brt_test_cdr`**: Тестируют обработку CDR-файлов сервисом BRT. Включают позитивные сценарии (корректные CDR), негативные (невалидные данные, такие как неверный тип вызова, неверный MSISDN, невалидные даты, окончание вызова раньше начала), граничные случаи (пустой CDR, звонки только между внешними номерами) и комплексные тесты со смешанными данными.
* **`e2e_tests`**: Проверяют сквозные сценарии биллинга для различных тарифных планов ("Классика", "Помесячный") и типов звонков (внутрисетевые, на внешние сети, входящие). Тесты включают проверку списания средств с баланса абонентов и расходования пакетных минут.
* **Более подробное описание каждого из этих тестовых проектов и их специфических сценариев находится в соответствующих README-файлах внутри их директорий (`auto-tests/cdr_generator/README.md`, `auto-tests/brt_test_cdr/README.md` и т.д.).**

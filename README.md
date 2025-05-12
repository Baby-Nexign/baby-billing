# Baby-Billing

## Описание

Baby-Billing — микросервисная биллинговая система для оператора "Ромашка"

## Документация

- [Swagger API](https://github.com/Baby-Nexign/baby-billing/blob/develop/swagger.yaml)
- [Архив с диаграммами и постановками](https://drive.google.com/drive/folders/1Gjx6BDM-7p8aNl7Oi4BmzQes1Nce9YPg?usp=sharing)

## Технологический стек

- **Языки и фреймворки**:
  - Java 17
  - Spring (Boot, Security, Data, Web, Cloud, AMQP, JPA)

- **Базы данных**:
  - PostgreSQL (для BRT, HRS и Auth сервисов)
  - H2 Database (для Commutator сервиса)

- **Брокер сообщений**:
  - RabbitMQ 

- **Инструменты**:
  - Liquibase
  - Lombok
  - Docker и Docker Compose
  - Maven

## Структура приложения

Приложение следует микросервисной архитектуре и состоит из следующих сервисов:

1. **Eureka Server** 
   - Сервер обнаружения и регистрации сервисов

2. **Auth Service** 
   - Управляет регистрацией и аутентификацией пользователей
   - Управляет выдачей ролей пользователям

3. **BRT Service** 
   - Основной биллинговый сервис
   - Хранение данных об абонентах, звонках и текущем балансе
   - Передача данных о звонках в HRS для расчета
   - Изменение баланса, на основе полученных данных
   - Управляет существующими абонентами, а также создает новых

4. **HRS Service**
   - Управление тарифами и услугами в них входящих

5. **Commutator Service**
   - Эмулирует коммутатор
   - Генерирует звонки и отправляет их в BRT в форме CDR отчетов

6. **CRM Service**
   - Выступает в роли API Gateway
   - Предоставляет REST API для клиентских приложений

## ERD диаграммы баз данных

<details>
<summary>BRT Schema</summary>
  
![brt](https://github.com/user-attachments/assets/03881678-1175-4484-9a61-c95481e28cda)
</details>

<details>
<summary>HRS Schema</summary>
  
![hrs](https://github.com/user-attachments/assets/ecc61043-da43-4557-9657-e64cc489ba5a)
</details>

<details>
<summary>Commutator Schema</summary>
  
</details>

<details>
<summary>Users Schema</summary>

![user](https://github.com/user-attachments/assets/70bfabe8-4ece-4fa2-97f8-d878dc78fdef)
</details>


## Схема взаимодействия сервисов

**Eureka Server** — сервер обнаружения и регистрации сервисов, с которым взаимодействуют все микросервисы.

**Основные взаимодействия:**

1. **CRM Service** → **Auth Service** 
   - Перенаправление API запросов

2. **CRM Service** → **Commutator Service**
   - Перенаправление API запросов

3. **CRM Service** → **BRT Service**
    - Перенаправление API запросов
    
4. **CRM Service** → **HRS Service**
    - Перенаправление API запросов
     
5. **Auth Service** → **BRT Service**
    - Получение информации о существовании абонента

6. **BRT Service** ↔ **HRS Service**
   - Обмен информацией о тарифах
   - Расчет стоимости звонков
   - Вычисление абонентской платы

7. **BRT Service** ↔ **Commutator Service**
   - Наложение ограничений на звонки
   - Получение CDR-записей
   - Создание новых абонентов

### RabbitMQ обмены сообщениями

| Exchange | Назначение                           | Взаимодействие |
|----------|--------------------------------------|----------------|
| **cdr-exchange** | Передача записей о вызовах (CDR)     | Commutator → BRT |
| **billing-request-exchange** | Запросы на расчет стоимости вызовов  | BRT → HRS |
| **billing-response-exchange** | Ответы с расчетами стоимости         | HRS → BRT |
| **call-restriction-request-exchange** | Наложение ограничений на вызовы      | BRT → Commutator |
| **tariff-information-request-exchange** | Запросы информации о тарифах         | BRT → HRS |
| **tariff-information-response-exchange** | Ответы с информацией о тарифах       | HRS → BRT |
| **new-subscriber-request-exchange** | Оповещение о новых абонентах         | BRT → Commutator |
| **count-tariff-payment-request-exchange** | Запросы на расчет абонентской платы  | BRT → HRS |
| **count-tariff-payment-response-exchange** | Ответы с расчетами абонентской платы | HRS → BRT |

### Цикл обработки звонка

1. Commutator генерирует запись о звонках (CDR)
2. BRT получает CDR и запрашивает расчет стоимости у HRS
3. HRS рассчитывает стоимость на основе тарифа и возвращает результат
4. BRT обновляет баланс абонента
5. BRT может наложить ограничения на вызовы через Commutator

## Запуск приложения

### С использованием Docker Compose

1. Убедитесь, что Docker и Docker Compose установлены в вашей системе
2. Перейдите в корневой каталог проекта
3. Выполните команду:
   ```
   docker-compose up -d
   ```

### Без Docker Compose

Для запуска каждого сервиса по отдельности:

1. Убедитесь, что у вас установлена JDK 17 

2. Сначала запустите зависимости:
   - Запустить контейнеры баз данных
   - Запустить контейнер rabbitmq

3. Установите переменные окружения для каждого сервиса cо значениями, указанными в docker-compose.yaml:
    - Параметры подключения к базе данных (POSTGRES_HOST, POSTGRES_PORT и т.д.)
    - Параметры подключения к RabbitMQ (RABBIT_HOST, RABBIT_PORT и т.д.)
    - Параметры подключения к Eureka (EUREKA_HOST, EUREKA_PORT)
    - Конфигурация JWT для Auth и СRM Service (JWT_SECRET, JWT_EXPIRATION и т.д.)
   
4. Для каждого сервиса перейдите в его каталог и выполните:
   ```
      ./mvnw spring-boot:run
   ```

      Запускайте сервисы в следующем порядке:
    1. Eureka Server
    2. Auth Service
    3. BRT Service
    4. HRS Service
    5. Commutator Service
    6. CRM Service

## Данные для авторизации

- Пользователь с ролью админ 
 
```
    login: admin
    password: admin
```

- Все абоненты имеют ассоциированных с ними пользователей, логин и пароль которых - их msisdn

```
    login: 79123456789
    password: 79123456789
```

## Коннект к базам данных

| Сервис | СУБД | Порт | База данных | Пользователь | Пароль |
|--------|------|------|------|--------------|--------|
| BRT Service | PostgreSQL | 5432 | brt | postgres | postgres |
| HRS Service | PostgreSQL | 5433 | hrs | postgres | postgres |
| Auth Service | PostgreSQL | 5434 | auth | postgres | postgres |
| Commutator Service | H2 (in-memory) | 8081 | commutator-service | su | password |

Для подключения к базам данных PostgreSQL через клиент:
1. Укажите соответствующий хост (`localhost`) и порт из таблицы выше
2. Введите имя базы данных, имя пользователя и пароль

Для подключения к базе данных Commutator Service через консоль H2:
1. Откройте браузер и перейдите по адресу: http://localhost:8081/h2-console
2. В поле JDBC URL введите: `jdbc:h2:mem:commutator-service`
3. В поле User Name введите: `su`
4. В поле Password введите: `password`
5. Нажмите Connect

## Особенности реализации (Разработка)

1. Поддерживается повторная генерация звонков на commutator-е со сдвигом на 1 год при каждом запросе. Однако при перезапуске контейнера отступ обновляется, что может привести к наложению звонков.

## Особенности реализации (Аналитика)

1. Роуминг и часовые пояса не учитываются
2. Базовый тариф для всех пользователей — "Классический"
3. Для упрощения проектирования некоторые списки реализованы словарями, некоторые через enum
4. Добавлен дополнительный сервис для авторизации пользователей
5. В булевых значениях: 0 — нет, 1 — да
6. Валюта системы — российские рубли (единая для всей системы)
7. "Помесячный" тариф реализован как "Классический" с входящей услугой пакета минут
8. Реализованы создание нового тарифа и механизм снятия абонентской платы
9. Абонентская плата снимается через фиксированные промежутки времени
10. Отказоустойчивость обеспечивается с помощью RabbitMQ
11. CDR (Call Detail Record) генерируется для всех вызовов

## Архив ручных тестов
На ранних этапах проекта были разработаны и проведены ручные тесты. В связи с дальнейшей эволюцией продукта и значительными изменениями в его функциональности, данные тесты могут быть неактуальны. Они сохраняются в архиве для истории и ознакомления с первоначальным видением тестирования.
https://drive.google.com/drive/folders/1mBgGJv-cDRfaMpqZMQ0Pbr7te_1JD_x3?usp=sharing

## Авторы

- [**Екатерина Кучкова**](https://github.com/venuels) - аналитика
- [**Васильев Константин**](https://github.com/KonstantinFrantz) - разработка
- [**Николай Именов**](https://github.com/imenov06) - тестирование

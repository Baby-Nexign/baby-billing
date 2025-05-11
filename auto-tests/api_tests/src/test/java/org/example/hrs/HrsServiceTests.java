package org.example.hrs;

import io.qameta.allure.*;
import io.qameta.allure.restassured.AllureRestAssured;
import io.restassured.http.ContentType;
import org.example.helpers.BaseApiTest;
import org.example.helpers.AuthHelper;
import org.example.helpers.TestDataGenerator;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import static io.restassured.RestAssured.given;
import static org.example.config.ApiTestConfig.*;
import static org.hamcrest.Matchers.*;

@Epic("HRS Сервис (Тарифы и Услуги)")
@Feature("API Услуг и Тарифов")
public class HrsServiceTests extends BaseApiTest {

    private static final String SERVICES_API_BASE_PATH = "/services";
    private static final String TARIFFS_API_BASE_PATH = "/tariffs";

    @Step("Получение токена администратора")
    private String getAdminTokenStep() {
        return AuthHelper.getAdminToken();
    }

    @Step("Получение токена абонента")
    private String getSubscriberTokenStep() {
        return AuthHelper.getSubscriberToken();
    }

    // ================================================
    // Тесты для Услуг (Services)
    // ================================================

    @Test
    @Tag("hrs")
    @Tag("services")
    @DisplayName("HRS-SERVICES-001: Неаутентифицированный пользователь успешно получает список услуг")
    @Description("Проверка, что любой пользователь (даже не аутентифицированный) может получить список всех доступных услуг.")
    @Severity(SeverityLevel.NORMAL)
    @Story("Получение списка услуг")
    public void unauthenticatedUserCanGetListOfServices() {
        Allure.step("Шаг 1: Отправка GET запроса на " + SERVICES_API_BASE_PATH + " без аутентификации");
        given()
                .filter(new AllureRestAssured())
                .when()
                .get(SERVICES_API_BASE_PATH)
                .then()
                .log().all()
                .statusCode(HTTP_OK)
                .contentType(ContentType.JSON)
                .body("$", instanceOf(java.util.List.class))
                .body("size()", greaterThanOrEqualTo(0));
        Allure.step("Шаг 2: Проверка успешного ответа (код 200, тип JSON, тело - список)");
    }

    @Test
    @Tag("hrs")
    @Tag("services")
    @DisplayName("HRS-SERVICES-002: Аутентифицированный администратор успешно получает список услуг")
    @Description("Проверка, что администратор может успешно получить список всех доступных услуг.")
    @Severity(SeverityLevel.NORMAL)
    @Story("Получение списка услуг")
    public void adminUserCanGetListOfServices() {
        String adminToken = getAdminTokenStep();
        Allure.parameter("Role", "ADMIN");

        Allure.step("Шаг 1: Отправка GET запроса на " + SERVICES_API_BASE_PATH + " от имени администратора");
        given()
                .filter(new AllureRestAssured())
                .header("Authorization", "Bearer " + adminToken)
                .when()
                .get(SERVICES_API_BASE_PATH)
                .then()
                .log().all()
                .statusCode(HTTP_OK)
                .contentType(ContentType.JSON)
                .body("$", instanceOf(java.util.List.class));
        Allure.step("Шаг 2: Проверка успешного ответа (код 200, тип JSON, тело - список)");
    }

    @Test
    @Tag("hrs")
    @Tag("services")
    @DisplayName("HRS-SERVICES-003: Неаутентифицированный пользователь успешно получает существующую услугу по ID")
    @Description("Проверка получения конкретной существующей услуги по ее ID без аутентификации.")
    @Severity(SeverityLevel.NORMAL)
    @Story("Получение услуги по ID")
    public void unauthenticatedUserCanGetExistingServiceById() {
        long existingServiceId = 1L; // ID услуги "50 минут"
        Allure.parameter("Service ID", existingServiceId);

        Allure.step("Шаг 1: Отправка GET запроса на " + SERVICES_API_BASE_PATH + "/{serviceId} для ID: " + existingServiceId);
        given()
                .filter(new AllureRestAssured())
                .pathParam("serviceId", existingServiceId)
                .when()
                .get(SERVICES_API_BASE_PATH + "/{serviceId}")
                .then()
                .log().all()
                .statusCode(HTTP_OK)
                .contentType(ContentType.JSON)
                .body("id", equalTo((int) existingServiceId))
                .body("name", equalTo("50 минут")); // Предполагаем, что услуга с ID=1 имеет имя "50 минут"
        Allure.step("Шаг 2: Проверка успешного ответа и соответствия данных услуги");
    }

    @Test
    @Tag("hrs")
    @Tag("services")
    @DisplayName("HRS-SERVICES-004: Запрос несуществующей услуги по ID возвращает 404 Not Found")
    @Description("Проверка, что при запросе услуги с несуществующим ID система возвращает ошибку 404.")
    @Severity(SeverityLevel.NORMAL)
    @Story("Получение услуги по ID (негативные сценарии)")
    public void getNonExistentServiceByIdReturnsNotFound() {
        long nonExistentServiceId = 99999L;
        Allure.parameter("Service ID", nonExistentServiceId);

        Allure.step("Шаг 1: Отправка GET запроса на " + SERVICES_API_BASE_PATH + "/{serviceId} для несуществующего ID: " + nonExistentServiceId);
        given()
                .filter(new AllureRestAssured())
                .pathParam("serviceId", nonExistentServiceId)
                .when()
                .get(SERVICES_API_BASE_PATH + "/{serviceId}")
                .then()
                .log().all()
                .spec(errorResponseSpec(HTTP_NOT_FOUND));
        Allure.step("Шаг 2: Проверка ответа об ошибке 'Не найдено' (код 404)");
    }

    @Test
    @Tag("hrs")
    @Tag("services")
    @DisplayName("HRS-SERVICES-005: Администратор успешно создает новую услугу (заглушка)")
    @Description("Проверка создания новой услуги администратором. Текущая реализация сервиса использует заглушку и возвращает 200 OK с null полями.")
    @Severity(SeverityLevel.NORMAL)
    @Story("Создание услуги")
    public void adminCanCreateNewService() {
        String adminToken = getAdminTokenStep();
        String serviceName = "New Test Service " + TestDataGenerator.generateUniqueMsisdn().substring(5);
        String serviceDescription = "Description for " + serviceName;
        Map<String, Object> requestBody = TestDataGenerator.createServiceRequestBody(serviceName, serviceDescription);

        Allure.step("Шаг 1: Формирование данных для создания новой услуги: " + serviceName);
        Allure.parameter("Service Name", serviceName);
        Allure.addAttachment("Тело запроса на создание услуги", "application/json", requestBody.toString(), ".json");

        Allure.step("Шаг 2: Отправка POST запроса на " + SERVICES_API_BASE_PATH + " от имени администратора");
        given()
                .filter(new AllureRestAssured())
                .spec(BASE_JSON_REQUEST_SPEC)
                .header("Authorization", "Bearer " + adminToken)
                .body(requestBody)
                .when()
                .post(SERVICES_API_BASE_PATH)
                .then()
                .log().all()
                .statusCode(HTTP_OK) // Ожидаем 200 OK из-за заглушки
                .contentType(ContentType.JSON)
                .body("id", nullValue())
                .body("name", nullValue());
        Allure.step("Шаг 3: Проверка ответа от заглушки (код 200, null поля)");
    }

    @Test
    @Tag("hrs")
    @Tag("services")
    @DisplayName("HRS-SERVICES-006: Абонент не может создать новую услугу (403 Forbidden)")
    @Description("Проверка запрета на создание новой услуги пользователем с ролью 'абонент'. Ожидается код 403.")
    @Severity(SeverityLevel.NORMAL)
    @Story("Создание услуги (негативные сценарии)")
    public void subscriberCannotCreateNewService() {
        String subscriberToken = getSubscriberTokenStep();
        Map<String, Object> requestBody = TestDataGenerator.createServiceRequestBody("Attempt Service", "Attempt Descr by Subscriber");

        Allure.step("Шаг 1: Формирование данных для попытки создания услуги");
        Allure.addAttachment("Тело запроса (попытка абонента)", "application/json", requestBody.toString(), ".json");

        Allure.step("Шаг 2: Отправка POST запроса на " + SERVICES_API_BASE_PATH + " от имени абонента");
        given()
                .filter(new AllureRestAssured())
                .spec(BASE_JSON_REQUEST_SPEC)
                .header("Authorization", "Bearer " + subscriberToken)
                .body(requestBody)
                .when()
                .post(SERVICES_API_BASE_PATH)
                .then()
                .log().all()
                .spec(errorResponseSpec(HTTP_FORBIDDEN));
        Allure.step("Шаг 3: Проверка ответа о запрете (код 403)");
    }

    // ================================================
    // Тесты для Тарифов (Tariffs)
    // ================================================

    @Test
    @Tag("hrs")
    @Tag("tariffs")
    @DisplayName("HRS-TARIFFS-001: Неаутентифицированный пользователь успешно получает список тарифов")
    @Description("Проверка, что любой пользователь (даже не аутентифицированный) может получить список всех доступных тарифов.")
    @Severity(SeverityLevel.NORMAL)
    @Story("Получение списка тарифов")
    public void unauthenticatedUserCanGetListOfTariffs() {
        Allure.step("Шаг 1: Отправка GET запроса на " + TARIFFS_API_BASE_PATH + " без аутентификации");
        given()
                .filter(new AllureRestAssured())
                .when()
                .get(TARIFFS_API_BASE_PATH)
                .then()
                .log().all()
                .statusCode(HTTP_OK)
                .contentType(ContentType.JSON)
                .body("$", instanceOf(java.util.List.class))
                .body("size()", greaterThanOrEqualTo(0));
        Allure.step("Шаг 2: Проверка успешного ответа (код 200, тип JSON, тело - список)");
    }

    @Test
    @Tag("hrs")
    @Tag("tariffs")
    @DisplayName("HRS-TARIFFS-002: Аутентифицированный администратор успешно получает список тарифов")
    @Description("Проверка, что администратор может успешно получить список всех доступных тарифов.")
    @Severity(SeverityLevel.NORMAL)
    @Story("Получение списка тарифов")
    public void adminUserCanGetListOfTariffs() {
        String adminToken = getAdminTokenStep();
        Allure.parameter("Role", "ADMIN");

        Allure.step("Шаг 1: Отправка GET запроса на " + TARIFFS_API_BASE_PATH + " от имени администратора");
        given()
                .filter(new AllureRestAssured())
                .header("Authorization", "Bearer " + adminToken)
                .when()
                .get(TARIFFS_API_BASE_PATH)
                .then()
                .log().all()
                .statusCode(HTTP_OK)
                .contentType(ContentType.JSON)
                .body("$", instanceOf(java.util.List.class));
        Allure.step("Шаг 2: Проверка успешного ответа (код 200, тип JSON, тело - список)");
    }

    @Test
    @Tag("hrs")
    @Tag("tariffs")
    @DisplayName("HRS-TARIFFS-003: Неаутентифицированный пользователь успешно получает существующий тариф по ID")
    @Description("Проверка получения конкретного существующего тарифа по его ID без аутентификации.")
    @Severity(SeverityLevel.NORMAL)
    @Story("Получение тарифа по ID")
    public void unauthenticatedUserCanGetExistingTariffById() {
        long existingTariffId = 11L; // ID тарифа "Классика"
        Allure.parameter("Tariff ID", existingTariffId);

        Allure.step("Шаг 1: Отправка GET запроса на " + TARIFFS_API_BASE_PATH + "/{tariffId} для ID: " + existingTariffId);
        given()
                .filter(new AllureRestAssured())
                .pathParam("tariffId", existingTariffId)
                .when()
                .get(TARIFFS_API_BASE_PATH + "/{tariffId}")
                .then()
                .log().all()
                .statusCode(HTTP_OK)
                .contentType(ContentType.JSON)
                .body("id", equalTo((int) existingTariffId))
                .body("name", equalTo("Классика"));
        Allure.step("Шаг 2: Проверка успешного ответа и соответствия данных тарифа");
    }

    @Test
    @Tag("hrs")
    @Tag("tariffs")
    @DisplayName("HRS-TARIFFS-004: Запрос несуществующего тарифа по ID возвращает 404 Not Found")
    @Description("Проверка, что при запросе тарифа с несуществующим ID система возвращает ошибку 404.")
    @Severity(SeverityLevel.NORMAL)
    @Story("Получение тарифа по ID (негативные сценарии)")
    public void getNonExistentTariffByIdReturnsNotFound() {
        long nonExistentTariffId = 88888L;
        Allure.parameter("Tariff ID", nonExistentTariffId);

        Allure.step("Шаг 1: Отправка GET запроса на " + TARIFFS_API_BASE_PATH + "/{tariffId} для несуществующего ID: " + nonExistentTariffId);
        given()
                .filter(new AllureRestAssured())
                .pathParam("tariffId", nonExistentTariffId)
                .when()
                .get(TARIFFS_API_BASE_PATH + "/{tariffId}")
                .then()
                .log().all()
                .spec(errorResponseSpec(HTTP_NOT_FOUND));
        Allure.step("Шаг 2: Проверка ответа об ошибке 'Не найдено' (код 404)");
    }

    @Test
    @Tag("hrs")
    @Tag("tariffs")
    @DisplayName("HRS-TARIFFS-005: Администратор успешно создает новый тариф")
    @Description("Проверка создания нового тарифа администратором с полным набором данных. Ожидается код 201 Created и возврат созданного тарифа.")
    @Severity(SeverityLevel.CRITICAL) // Теперь это критичный тест
    @Story("Создание тарифа")
    public void adminCanCreateNewTariff() {
        String adminToken = getAdminTokenStep();

        String tariffName = "Тестовый Безлимит " + TestDataGenerator.generateUniqueMsisdn().substring(8);
        Integer paymentPeriod = 30;
        Integer cost = 2000;
        String tariffDescription = "Описание для тарифа '" + tariffName + "'";
        List<Long> serviceIds = Collections.singletonList(1L);
        Map<String, Integer> callPrices = TestDataGenerator.createCallPrices(0, 0, 50, 150); // 0, 0, 0.5 руб, 1.5 руб (если цены в копейках)

        Map<String, Object> requestBody = TestDataGenerator.createFullTariffRequestBody(
                tariffName, paymentPeriod, cost, tariffDescription, serviceIds, callPrices
        );

        Allure.step("Шаг 1: Формирование данных для создания нового тарифа: " + tariffName);
        Allure.parameter("Tariff Name", tariffName);
        Allure.parameter("Payment Period", paymentPeriod);
        Allure.parameter("Cost", cost);
        Allure.parameter("Service IDs", serviceIds.toString());
        Allure.addAttachment("Тело запроса на создание тарифа", "application/json", requestBody.toString(), ".json");

        Allure.step("Шаг 2: Отправка POST запроса на " + TARIFFS_API_BASE_PATH + " от имени администратора");

        // Сохраняем ответ для более детальной проверки и возможности извлечь ID
        io.restassured.response.Response response = given()
                .filter(new AllureRestAssured())
                .spec(BASE_JSON_REQUEST_SPEC)
                .header("Authorization", "Bearer " + adminToken)
                .body(requestBody)
                .when()
                .post(TARIFFS_API_BASE_PATH)
                .then()
                .log().all()
                .statusCode(HTTP_CREATED) // Ожидаем 201 Created
                .contentType(ContentType.JSON)
                // Проверяем основные поля в ответе
                .body("name", equalTo(tariffName))
                .body("paymentPeriod", equalTo(paymentPeriod))
                .body("cost", equalTo(cost))
                .body("description", equalTo(tariffDescription))
                .body("serviceIds", equalTo(serviceIds.stream().map(Long::intValue).collect(java.util.stream.Collectors.toList()))) // Сравниваем списки, может потребоваться преобразование Long в Integer если JSON возвращает числа
                .body("callPrices.incomingInNetworkPrice", equalTo(callPrices.get("incomingInNetworkPrice")))
                .body("callPrices.outgoingInNetworkPrice", equalTo(callPrices.get("outgoingInNetworkPrice")))
                .body("callPrices.incomingOutNetworkPrice", equalTo(callPrices.get("incomingOutNetworkPrice")))
                .body("callPrices.outgoingOutNetworkPrice", equalTo(callPrices.get("outgoingOutNetworkPrice")))
                .body("id", notNullValue())
                .extract().response();

        int createdTariffId = response.jsonPath().getInt("id");
        Allure.step("Шаг 3: Проверка успешного ответа (код 201 Created, тело ответа соответствует запросу, ID тарифа: " + createdTariffId + ")");
        Allure.addAttachment("Тело ответа (Созданный тариф)", "application/json", response.asPrettyString(), ".json");
    }

    @Test
    @Tag("hrs")
    @Tag("tariffs")
    @DisplayName("HRS-TARIFFS-006: Абонент не может создать новый тариф (403 Forbidden)")
    @Description("Проверка запрета на создание нового тарифа пользователем с ролью 'абонент'. Ожидается код 403.")
    @Severity(SeverityLevel.NORMAL)
    @Story("Создание тарифа (негативные сценарии)")
    public void subscriberCannotCreateNewTariff() {
        String subscriberToken = getSubscriberTokenStep();
        Map<String, Object> requestBody = TestDataGenerator.createTariffRequestBody("Attempt Tariff", "Attempt Descr by Subscriber");

        Allure.step("Шаг 1: Формирование данных для попытки создания тарифа");
        Allure.addAttachment("Тело запроса (попытка абонента)", "application/json", requestBody.toString(), ".json");

        Allure.step("Шаг 2: Отправка POST запроса на " + TARIFFS_API_BASE_PATH + " от имени абонента");
        given()
                .filter(new AllureRestAssured())
                .spec(BASE_JSON_REQUEST_SPEC)
                .header("Authorization", "Bearer " + subscriberToken)
                .body(requestBody)
                .when()
                .post(TARIFFS_API_BASE_PATH)
                .then()
                .log().all()
                .spec(errorResponseSpec(HTTP_FORBIDDEN));
        Allure.step("Шаг 3: Проверка ответа о запрете (код 403)");
    }
}
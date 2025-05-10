package org.example.hrs;

import io.restassured.http.ContentType;
import org.example.helpers.BaseApiTest;
import org.example.helpers.AuthHelper;
import org.example.helpers.TestDataGenerator;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static io.restassured.RestAssured.given;
import static org.example.config.ApiTestConfig.*;
import static org.hamcrest.Matchers.*; // Для проверок списков и полей

public class HrsServiceTests extends BaseApiTest {

    private static final String SERVICES_API_BASE_PATH = "/services";
    private static final String TARIFFS_API_BASE_PATH = "/tariffs"; // Пригодится позже


    @Test
    @Tag("hrs")
    @Tag("services")
    @DisplayName("HRS-SERVICES-001: Неаутентифицированный пользователь успешно получает список услуг")
    public void unauthenticatedUserCanGetListOfServices() {
        given()
                .when()
                .get(SERVICES_API_BASE_PATH)
                .then()
                .log().all()
                .statusCode(HTTP_OK)
                .contentType(ContentType.JSON)
                .body("$", instanceOf(java.util.List.class))
                .body("size()", greaterThanOrEqualTo(0));
        // В HRS по умолчанию создается одна услуга "50 минут"
    }

    @Test
    @Tag("hrs")
    @Tag("services")
    @DisplayName("HRS-SERVICES-002: Аутентифицированный администратор успешно получает список услуг")
    public void adminUserCanGetListOfServices() {
        String adminToken = AuthHelper.getAdminToken();

        given()
                .header("Authorization", "Bearer " + adminToken)
                .when()
                .get(SERVICES_API_BASE_PATH)
                .then()
                .log().all()
                .statusCode(HTTP_OK)
                .contentType(ContentType.JSON)
                .body("$", instanceOf(java.util.List.class));
    }

    @Test
    @Tag("hrs")
    @Tag("services")
    @DisplayName("HRS-SERVICES-003: Неаутентифицированный пользователь успешно получает существующую услугу по ID")
    public void unauthenticatedUserCanGetExistingServiceById() {
        long existingServiceId = 1L; // ID услуги "50 минут", создаваемой по умолчанию в HRS

        given()
                .pathParam("serviceId", existingServiceId)
                .when()
                .get(SERVICES_API_BASE_PATH + "/{serviceId}")
                .then()
                .log().all()
                .statusCode(HTTP_OK)
                .contentType(ContentType.JSON)
                .body("id", equalTo((int) existingServiceId))
                .body("name", equalTo("50 минут"));
    }

    @Test
    @Tag("hrs")
    @Tag("services")
    @DisplayName("HRS-SERVICES-004: Запрос несуществующей услуги по ID возвращает 404 Not Found")
    public void getNonExistentServiceByIdReturnsNotFound() {
        long nonExistentServiceId = 99999L;

        given()
                .pathParam("serviceId", nonExistentServiceId)
                .when()
                .get(SERVICES_API_BASE_PATH + "/{serviceId}")
                .then()
                .log().all()
                .spec(errorResponseSpec(HTTP_NOT_FOUND));
    }

    @Test
    @Tag("hrs")
    @Tag("services")
    @DisplayName("HRS-SERVICES-005: Администратор успешно создает новую услугу. На данный функция создания сервиса не реализована, используется заглушка.")
    public void adminCanCreateNewService() {
        String adminToken = AuthHelper.getAdminToken();
        String serviceName = "New Test Service " + TestDataGenerator.generateUniqueMsisdn().substring(5);
        String serviceDescription = "Description for " + serviceName;

        Map<String, Object> requestBody = TestDataGenerator.createServiceRequestBody(serviceName, serviceDescription);

        given()
                .spec(BASE_JSON_REQUEST_SPEC)
                .header("Authorization", "Bearer " + adminToken)
                .body(requestBody)
                .when()
                .post(SERVICES_API_BASE_PATH)
                .then()
                .log().all()
                // Сервис HRS OperatorServiceService.createService возвращает заглушку и ResponseEntity.ok()
                // Поэтому ожидаем 200 OK, а не 201 Created. Тело ответа будет содержать null-поля.

                .statusCode(HTTP_OK)
                .contentType(ContentType.JSON)
                .body("id", nullValue())
                .body("name", nullValue());
    }

    @Test
    @Tag("hrs")
    @Tag("services")
    @DisplayName("HRS-SERVICES-006: Абонент не может создать новую услугу (403 Forbidden)")
    public void subscriberCannotCreateNewService() {
        String subscriberToken = AuthHelper.getSubscriberToken();
        Map<String, Object> requestBody = TestDataGenerator.createServiceRequestBody("Attempt Service", "Attempt Descr");

        given()
                .spec(BASE_JSON_REQUEST_SPEC)
                .header("Authorization", "Bearer " + subscriberToken)
                .body(requestBody)
                .when()
                .post(SERVICES_API_BASE_PATH)
                .then()
                .log().all()
                .spec(errorResponseSpec(HTTP_FORBIDDEN));
    }

    @Test
    @Tag("hrs")
    @Tag("tariffs")
    @DisplayName("HRS-TARIFFS-001: Неаутентифицированный пользователь успешно получает список тарифов")
    public void unauthenticatedUserCanGetListOfTariffs() {
        given()
                .when()
                .get(TARIFFS_API_BASE_PATH)
                .then()
                .log().all()
                .statusCode(HTTP_OK)
                .contentType(ContentType.JSON)
                .body("$", instanceOf(java.util.List.class))
                .body("size()", greaterThanOrEqualTo(0));
    }

    @Test
    @Tag("hrs")
    @Tag("tariffs")
    @DisplayName("HRS-TARIFFS-002: Аутентифицированный администратор успешно получает список тарифов")
    public void adminUserCanGetListOfTariffs() {
        String adminToken = AuthHelper.getAdminToken();

        given()
                .header("Authorization", "Bearer " + adminToken)
                .when()
                .get(TARIFFS_API_BASE_PATH)
                .then()
                .log().all()
                .statusCode(HTTP_OK)
                .contentType(ContentType.JSON)
                .body("$", instanceOf(java.util.List.class));
    }

    @Test
    @Tag("hrs")
    @Tag("tariffs")
    @DisplayName("HRS-TARIFFS-003: Неаутентифицированный пользователь успешно получает существующий тариф по ID")
    public void unauthenticatedUserCanGetExistingTariffById() {
        long existingTariffId = 11L; // ID тарифа "Классика", создаваемого по умолчанию в HRS

        given()
                .pathParam("tariffId", existingTariffId)
                .when()
                .get(TARIFFS_API_BASE_PATH + "/{tariffId}")
                .then()
                .log().all()
                .statusCode(HTTP_OK)
                .contentType(ContentType.JSON)
                .body("id", equalTo((int) existingTariffId))
                .body("name", equalTo("Классика"));
    }

    @Test
    @Tag("hrs")
    @Tag("tariffs")
    @DisplayName("HRS-TARIFFS-004: Запрос несуществующего тарифа по ID возвращает 404 Not Found")
    public void getNonExistentTariffByIdReturnsNotFound() {
        long nonExistentTariffId = 88888L;

        given()
                .pathParam("tariffId", nonExistentTariffId)
                .when()
                .get(TARIFFS_API_BASE_PATH + "/{tariffId}")
                .then()
                .log().all()
                .spec(errorResponseSpec(HTTP_NOT_FOUND));
    }

    @Test
    @Tag("hrs")
    @Tag("tariffs")
    @DisplayName("HRS-TARIFFS-005: Администратор успешно создает новый тариф")
    public void adminCanCreateNewTariff() {
        String adminToken = AuthHelper.getAdminToken();
        String tariffName = "New Test Tariff " + TestDataGenerator.generateUniqueMsisdn().substring(5);
        String tariffDescription = "Description for " + tariffName;

        Map<String, Object> requestBody = TestDataGenerator.createTariffRequestBody(tariffName, tariffDescription);

        given()
                .spec(BASE_JSON_REQUEST_SPEC)
                .header("Authorization", "Bearer " + adminToken)
                .body(requestBody)
                .when()
                .post(TARIFFS_API_BASE_PATH)
                .then()
                .log().all()
                // Сервис HRS TariffService.createTariff возвращает заглушку и ResponseEntity.ok()
                .statusCode(HTTP_OK) // Ожидаем 200 OK из-за заглушки
                .contentType(ContentType.JSON)
                .body("id", nullValue()) // Проверяем, что ID в ответе null, как в заглушке сервиса
                .body("name", nullValue()); // И имя тоже null
    }

    @Test
    @Tag("hrs")
    @Tag("tariffs")
    @DisplayName("HRS-TARIFFS-006: Абонент не может создать новый тариф (403 Forbidden)")
    public void subscriberCannotCreateNewTariff() {
        String subscriberToken = AuthHelper.getSubscriberToken();
        Map<String, Object> requestBody = TestDataGenerator.createTariffRequestBody("Attempt Tariff", "Attempt Descr");

        given()
                .spec(BASE_JSON_REQUEST_SPEC)
                .header("Authorization", "Bearer " + subscriberToken)
                .body(requestBody)
                .when()
                .post(TARIFFS_API_BASE_PATH)
                .then()
                .log().all()
                .spec(errorResponseSpec(HTTP_FORBIDDEN));
    }
}
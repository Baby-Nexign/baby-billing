package org.example.brt;

import org.example.helpers.BaseApiTest;
import org.example.helpers.AuthHelper;
import org.example.helpers.TestDataGenerator;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static io.restassured.RestAssured.given;
import static org.example.config.ApiTestConfig.*;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.notNullValue;

public class BrtPersonTests extends BaseApiTest {
    @Test
    @Tag("brt")
    @Tag("person")
    @DisplayName("BRT-PERSON-001: Администратор успешно создает нового абонента")
    public void adminCanCreateNewPersonSuccessfully() {
        String adminToken = AuthHelper.getAdminToken();
        String newMsisdn = TestDataGenerator.generateUniqueMsisdn();
        String name = "TestPerson_" + newMsisdn.substring(newMsisdn.length() - 4);
        String description = "Auto-created test person " + name;

        Map<String, Object> requestBody = TestDataGenerator.createPersonRequestBody(name, newMsisdn, description);

        given()
                .spec(BASE_JSON_REQUEST_SPEC)
                .header("Authorization", "Bearer " + adminToken)
                .body(requestBody)
                .when()
                .post(PERSON_API_BASE_PATH)
                .then()
                .log().all()
                .statusCode(HTTP_CREATED)
                .body("msisdn", equalTo(newMsisdn))
                .body("name", equalTo(name))
                .body("description", equalTo(description))
                .body("balance", notNullValue())
                .body("isRestricted", equalTo(false));
    }

    @Test
    @Tag("brt")
    @Tag("person")
    @DisplayName("BRT-PERSON-002: Абонент не может создать нового абонента (403 Forbidden)")
    public void subscriberCannotCreateNewPerson() {
        String subscriberToken = AuthHelper.getSubscriberToken();
        String newMsisdn = TestDataGenerator.generateUniqueMsisdn();
        String name = "AttemptPerson_" + newMsisdn.substring(newMsisdn.length() - 4);
        String description = "Attempted auto-created test person " + name;

        Map<String, Object> requestBody = TestDataGenerator.createPersonRequestBody(name, newMsisdn, description);

        given()
                .spec(BASE_JSON_REQUEST_SPEC)
                .header("Authorization", "Bearer " + subscriberToken)
                .body(requestBody)
                .when()
                .post(PERSON_API_BASE_PATH)
                .then()
                .log().all()
                .spec(errorResponseSpec(HTTP_FORBIDDEN));
    }

    @Test
    @Tag("brt")
    @Tag("person")
    @DisplayName("BRT-PERSON-003: Администратор успешно меняет тариф абоненту")
    public void adminCanChangePersonTariffSuccessfully() {
        String adminToken = AuthHelper.getAdminToken();
        String targetMsisdn = PREDEFINED_SUBSCRIBER_MSISDN_1;
        Long newTariffId = 12L;

        Map<String, Object> requestBody = TestDataGenerator.createChangePersonTariffRequestBody(targetMsisdn, newTariffId);

        given()
                .spec(BASE_JSON_REQUEST_SPEC)
                .header("Authorization", "Bearer " + adminToken)
                .body(requestBody)
                .when()
                .put(PERSON_API_BASE_PATH + "/tariff")
                .then()
                .log().all()
                .statusCode(HTTP_OK);
    }

    @Test
    @Tag("brt")
    @Tag("person")
    @DisplayName("BRT-PERSON-004: Абонент успешно меняет свой собственный тариф")
    public void subscriberCanChangeOwnTariffSuccessfully() {
        String subscriberToken = AuthHelper.getSubscriberToken(
                PREDEFINED_SUBSCRIBER_MSISDN_1,
                PREDEFINED_SUBSCRIBER_PASSWORD_1
        );
        String ownMsisdn = PREDEFINED_SUBSCRIBER_MSISDN_1;
        Long newTariffId = 11L;

        Map<String, Object> requestBody = TestDataGenerator.createChangePersonTariffRequestBody(ownMsisdn, newTariffId);

        given()
                .spec(BASE_JSON_REQUEST_SPEC)
                .header("Authorization", "Bearer " + subscriberToken)
                .body(requestBody)
                .when()
                .put(PERSON_API_BASE_PATH + "/tariff")
                .then()
                .log().all()
                .statusCode(HTTP_OK);
    }

    @Test
    @Tag("brt")
    @Tag("person")
    @DisplayName("BRT-PERSON-005: Абонент не может сменить тариф другому абоненту (403 Forbidden)")
    public void subscriberCannotChangeTariffForAnotherPerson() {
        String actingSubscriberToken = AuthHelper.getSubscriberToken(
                PREDEFINED_SUBSCRIBER_MSISDN_1,
                PREDEFINED_SUBSCRIBER_PASSWORD_1
        );
        String otherPersonMsisdn = TestDataGenerator.generateUniqueMsisdn();
        Long newTariffId = 12L;

        Map<String, Object> requestBody = TestDataGenerator.createChangePersonTariffRequestBody(otherPersonMsisdn, newTariffId);

        given()
                .spec(BASE_JSON_REQUEST_SPEC)
                .header("Authorization", "Bearer " + actingSubscriberToken)
                .body(requestBody)
                .when()
                .put(PERSON_API_BASE_PATH + "/tariff")
                .then()
                .log().all()
                .spec(errorResponseSpec(HTTP_FORBIDDEN));
    }

    @Test
    @Tag("brt")
    @Tag("person")
    @DisplayName("BRT-PERSON-006: Администратор успешно пополняет баланс абоненту")
    public void adminCanReplenishPersonBalanceSuccessfully() {
        String adminToken = AuthHelper.getAdminToken();
        String targetMsisdn = PREDEFINED_SUBSCRIBER_MSISDN_1;
        long amountToReplenish = 504L;

        given()
                .spec(BASE_JSON_REQUEST_SPEC)
                .header("Authorization", "Bearer " + adminToken)
                .pathParam("msisdn", targetMsisdn)
                .queryParam("money", amountToReplenish)
                .when()
                .put(PERSON_API_BASE_PATH + "/{msisdn}/balance")
                .then()
                .log().all()
                .statusCode(HTTP_OK);
    }

    @Test
    @Tag("brt")
    @Tag("person")
    @DisplayName("BRT-PERSON-007: Абонент успешно пополняет свой собственный баланс")
    public void subscriberCanReplenishOwnBalanceSuccessfully() {
        String subscriberToken = AuthHelper.getSubscriberToken(
                PREDEFINED_SUBSCRIBER_MSISDN_2,
                PREDEFINED_SUBSCRIBER_PASSWORD_2
        );
        String ownMsisdn = PREDEFINED_SUBSCRIBER_MSISDN_2;
        long amountToReplenish = 779L;

        given()
                .spec(BASE_JSON_REQUEST_SPEC)
                .header("Authorization", "Bearer " + subscriberToken)
                .pathParam("msisdn", ownMsisdn)
                .queryParam("money", amountToReplenish)
                .when()
                .put(PERSON_API_BASE_PATH + "/{msisdn}/balance")
                .then()
                .log().all()
                .statusCode(HTTP_OK);
    }

    @Test
    @Tag("brt")
    @Tag("person")
    @DisplayName("BRT-PERSON-008: Абонент не может пополнить баланс другому абоненту (403 Forbidden)")
    public void subscriberCannotReplenishBalanceForAnotherPerson() {
        String actingSubscriberToken = AuthHelper.getSubscriberToken(
                PREDEFINED_SUBSCRIBER_MSISDN_1,
                PREDEFINED_SUBSCRIBER_PASSWORD_1
        );
        String otherPersonMsisdn = TestDataGenerator.generateUniqueMsisdn();
        long amountToReplenish = 75L;

        given()
                .spec(BASE_JSON_REQUEST_SPEC)
                .header("Authorization", "Bearer " + actingSubscriberToken)
                .pathParam("msisdn", otherPersonMsisdn)
                .queryParam("money", amountToReplenish)
                .when()
                .put(PERSON_API_BASE_PATH + "/{msisdn}/balance")
                .then()
                .log().all()
                .spec(errorResponseSpec(HTTP_FORBIDDEN));
    }

    @Test
    @Tag("brt")
    @Tag("person")
    @DisplayName("BRT-PERSON-009: Администратор успешно получает информацию об абоненте")
    public void adminCanGetPersonInfoSuccessfully() {
        String adminToken = AuthHelper.getAdminToken();
        String targetMsisdn = PREDEFINED_SUBSCRIBER_MSISDN_1; // MSISDN существующего абонента
        given()
                .spec(BASE_JSON_REQUEST_SPEC)
                .header("Authorization", "Bearer " + adminToken)
                .pathParam("msisdn", targetMsisdn)
                .when()
                .get(PERSON_API_BASE_PATH + "/{msisdn}")
                .then()
                .log().all()
                .statusCode(HTTP_OK)
                .body("msisdn", equalTo(targetMsisdn)) // Проверяем ключевые поля в ответе
                .body("name", notNullValue())
                .body("balance", notNullValue())
                .body("isRestricted", notNullValue())
                .body("tariff.tariffId", notNullValue()); // Пример проверки вложенного поля
    }

    @Test
    @Tag("brt")
    @Tag("person")
    @DisplayName("BRT-PERSON-010: Абонент успешно получает информацию о себе")
    public void subscriberCanGetOwnInfoSuccessfully() {
        String subscriberToken = AuthHelper.getSubscriberToken(
                PREDEFINED_SUBSCRIBER_MSISDN_1,
                PREDEFINED_SUBSCRIBER_PASSWORD_1
        );
        String ownMsisdn = PREDEFINED_SUBSCRIBER_MSISDN_1;

        given()
                .spec(BASE_JSON_REQUEST_SPEC)
                .header("Authorization", "Bearer " + subscriberToken)
                .pathParam("msisdn", ownMsisdn)
                .when()
                .get(PERSON_API_BASE_PATH + "/{msisdn}")
                .then()
                .log().all()
                .statusCode(HTTP_OK)
                .body("msisdn", equalTo(ownMsisdn))
                .body("name", notNullValue());
    }

    @Test
    @Tag("brt")
    @Tag("person")
    @DisplayName("BRT-PERSON-011: Абонент не может получить информацию о другом абоненте (403 Forbidden)")
    public void subscriberCannotGetInfoForAnotherPerson() {
        String actingSubscriberToken = AuthHelper.getSubscriberToken(
                PREDEFINED_SUBSCRIBER_MSISDN_1,
                PREDEFINED_SUBSCRIBER_PASSWORD_1
        );
        String otherPersonMsisdn = TestDataGenerator.generateUniqueMsisdn();
        given()
                .spec(BASE_JSON_REQUEST_SPEC)
                .header("Authorization", "Bearer " + actingSubscriberToken)
                .pathParam("msisdn", otherPersonMsisdn)
                .when()
                .get(PERSON_API_BASE_PATH + "/{msisdn}")
                .then()
                .log().all()
                .spec(errorResponseSpec(HTTP_FORBIDDEN)); // Ожидаем 403 Forbidden
    }

    @Test
    @Tag("brt")
    @Tag("person")
    @DisplayName("BRT-PERSON-012: Попытка получить информацию о несуществующем абоненте (404 Not Found)")
    public void getInfoForNonExistentPersonReturnsNotFound() {
        String adminToken = AuthHelper.getAdminToken();
        String nonExistentMsisdn = TestDataGenerator.generateUniqueMsisdn();

        given()
                .spec(BASE_JSON_REQUEST_SPEC)
                .header("Authorization", "Bearer " + adminToken)
                .pathParam("msisdn", nonExistentMsisdn)
                .when()
                .get(PERSON_API_BASE_PATH + "/{msisdn}")
                .then()
                .log().all()
                .spec(errorResponseSpec(HTTP_NOT_FOUND));
    }
}
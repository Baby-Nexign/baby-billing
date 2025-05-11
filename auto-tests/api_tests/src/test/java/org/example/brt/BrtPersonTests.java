package org.example.brt;

import io.qameta.allure.*;
import io.qameta.allure.restassured.AllureRestAssured;
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

@Epic("BRT Сервис (Управление абонентами)")
@Feature("API Абонентов (Person API)")
public class BrtPersonTests extends BaseApiTest {

    @Step("Получение токена администратора")
    private String getAdminTokenStep() {
        String token = AuthHelper.getAdminToken();
        Allure.addAttachment("Admin Token (частично)", "text/plain", token.substring(0, Math.min(token.length(), 20)) + "...", ".txt");
        return token;
    }

    @Step("Получение токена абонента для MSISDN: {msisdn}")
    private String getSubscriberTokenStep(String msisdn, String password) {
        Allure.parameter("MSISDN", msisdn);
        String token = AuthHelper.getSubscriberToken(msisdn, password);
        Allure.addAttachment("Subscriber Token (частично) for " + msisdn, "text/plain", token.substring(0, Math.min(token.length(), 20)) + "...", ".txt");
        return token;
    }

    @Test
    @Tag("brt")
    @Tag("person")
    @DisplayName("BRT-PERSON-001: Администратор успешно создает нового абонента")
    @Description("Проверка возможности создания нового абонента администратором. Ожидается код 201 и корректное тело ответа.")
    @Severity(SeverityLevel.CRITICAL)
    @Story("Создание абонента")
    public void adminCanCreateNewPersonSuccessfully() {
        String adminToken = getAdminTokenStep();

        String newMsisdn = TestDataGenerator.generateUniqueMsisdn();
        String name = "TestPerson_" + newMsisdn.substring(newMsisdn.length() - 4);
        String description = "Auto-created test person " + name;
        Map<String, Object> requestBody = TestDataGenerator.createPersonRequestBody(name, newMsisdn, description);

        Allure.step("Шаг 1: Формирование данных для создания абонента (MSISDN: " + newMsisdn + ")");
        Allure.parameter("MSISDN", newMsisdn);
        Allure.parameter("Name", name);
        Allure.addAttachment("Тело запроса на создание", "application/json", requestBody.toString(), ".json");

        Allure.step("Шаг 2: Отправка POST запроса на " + PERSON_API_BASE_PATH);
        given()
                .filter(new AllureRestAssured())
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
        Allure.step("Шаг 3: Проверка успешного ответа (код 201 и тело ответа)");
    }

    @Test
    @Tag("brt")
    @Tag("person")
    @DisplayName("BRT-PERSON-002: Абонент не может создать нового абонента (403 Forbidden)")
    @Description("Проверка запрета на создание нового абонента пользователем с ролью 'абонент'. Ожидается код 403.")
    @Severity(SeverityLevel.NORMAL)
    @Story("Создание абонента (негативные сценарии)")
    public void subscriberCannotCreateNewPerson() {
        String subscriberToken = getSubscriberTokenStep(PREDEFINED_SUBSCRIBER_MSISDN_1, PREDEFINED_SUBSCRIBER_PASSWORD_1);

        String newMsisdn = TestDataGenerator.generateUniqueMsisdn();
        String name = "AttemptPerson_" + newMsisdn.substring(newMsisdn.length() - 4);
        Map<String, Object> requestBody = TestDataGenerator.createPersonRequestBody(name, newMsisdn, "Attempted auto-created");

        Allure.step("Шаг 1: Формирование данных для попытки создания абонента (MSISDN: " + newMsisdn + ")");
        Allure.addAttachment("Тело запроса на создание (попытка абонента)", "application/json", requestBody.toString(), ".json");

        Allure.step("Шаг 2: Отправка POST запроса на " + PERSON_API_BASE_PATH + " от имени абонента");
        given()
                .filter(new AllureRestAssured())
                .spec(BASE_JSON_REQUEST_SPEC)
                .header("Authorization", "Bearer " + subscriberToken)
                .body(requestBody)
                .when()
                .post(PERSON_API_BASE_PATH)
                .then()
                .log().all()
                .spec(errorResponseSpec(HTTP_FORBIDDEN));
        Allure.step("Шаг 3: Проверка ответа о запрете (код 403)");
    }

    @Test
    @Tag("brt")
    @Tag("person")
    @DisplayName("BRT-PERSON-003: Администратор успешно меняет тариф абоненту")
    @Description("Проверка возможности смены тарифа существующему абоненту администратором. Ожидается код 200.")
    @Severity(SeverityLevel.CRITICAL)
    @Story("Изменение тарифа абонента")
    public void adminCanChangePersonTariffSuccessfully() {
        String adminToken = getAdminTokenStep();
        String targetMsisdn = PREDEFINED_SUBSCRIBER_MSISDN_1;
        Long newTariffId = 12L;
        Map<String, Object> requestBody = TestDataGenerator.createChangePersonTariffRequestBody(targetMsisdn, newTariffId);

        Allure.step("Шаг 1: Формирование данных для смены тарифа (MSISDN: " + targetMsisdn + ", New Tariff ID: " + newTariffId + ")");
        Allure.parameter("Target MSISDN", targetMsisdn);
        Allure.parameter("New Tariff ID", newTariffId);
        Allure.addAttachment("Тело запроса на смену тарифа", "application/json", requestBody.toString(), ".json");

        Allure.step("Шаг 2: Отправка PUT запроса на " + PERSON_API_BASE_PATH + "/tariff");
        given()
                .filter(new AllureRestAssured())
                .spec(BASE_JSON_REQUEST_SPEC)
                .header("Authorization", "Bearer " + adminToken)
                .body(requestBody)
                .when()
                .put(PERSON_API_BASE_PATH + "/tariff")
                .then()
                .log().all()
                .statusCode(HTTP_OK);
        Allure.step("Шаг 3: Проверка успешного ответа (код 200)");
    }

    @Test
    @Tag("brt")
    @Tag("person")
    @DisplayName("BRT-PERSON-004: Абонент успешно меняет свой собственный тариф")
    @Description("Проверка возможности смены собственного тарифа абонентом. Ожидается код 200.")
    @Severity(SeverityLevel.CRITICAL)
    @Story("Изменение тарифа абонента")
    public void subscriberCanChangeOwnTariffSuccessfully() {
        String subscriberToken = getSubscriberTokenStep(PREDEFINED_SUBSCRIBER_MSISDN_1, PREDEFINED_SUBSCRIBER_PASSWORD_1);
        String ownMsisdn = PREDEFINED_SUBSCRIBER_MSISDN_1;
        Long newTariffId = 11L;
        Map<String, Object> requestBody = TestDataGenerator.createChangePersonTariffRequestBody(ownMsisdn, newTariffId);

        Allure.step("Шаг 1: Формирование данных для смены собственного тарифа (MSISDN: " + ownMsisdn + ", New Tariff ID: " + newTariffId + ")");
        Allure.parameter("Own MSISDN", ownMsisdn);
        Allure.parameter("New Tariff ID", newTariffId);
        Allure.addAttachment("Тело запроса на смену тарифа (самостоятельно)", "application/json", requestBody.toString(), ".json");

        Allure.step("Шаг 2: Отправка PUT запроса на " + PERSON_API_BASE_PATH + "/tariff от имени абонента");
        given()
                .filter(new AllureRestAssured())
                .spec(BASE_JSON_REQUEST_SPEC)
                .header("Authorization", "Bearer " + subscriberToken)
                .body(requestBody)
                .when()
                .put(PERSON_API_BASE_PATH + "/tariff")
                .then()
                .log().all()
                .statusCode(HTTP_OK);
        Allure.step("Шаг 3: Проверка успешного ответа (код 200)");
    }

    @Test
    @Tag("brt")
    @Tag("person")
    @DisplayName("BRT-PERSON-005: Абонент не может сменить тариф другому абоненту (403 Forbidden)")
    @Description("Проверка запрета на смену тарифа другому абоненту пользователем с ролью 'абонент'. Ожидается код 403.")
    @Severity(SeverityLevel.NORMAL)
    @Story("Изменение тарифа абонента (негативные сценарии)")
    public void subscriberCannotChangeTariffForAnotherPerson() {
        String actingSubscriberToken = getSubscriberTokenStep(PREDEFINED_SUBSCRIBER_MSISDN_1, PREDEFINED_SUBSCRIBER_PASSWORD_1);
        String otherPersonMsisdn = PREDEFINED_SUBSCRIBER_MSISDN_2; // Используем другого предопределенного абонента
        Long newTariffId = 12L;
        Map<String, Object> requestBody = TestDataGenerator.createChangePersonTariffRequestBody(otherPersonMsisdn, newTariffId);

        Allure.step("Шаг 1: Формирование данных для попытки смены тарифа другому абоненту (Target MSISDN: " + otherPersonMsisdn + ")");
        Allure.parameter("Acting Subscriber MSISDN", PREDEFINED_SUBSCRIBER_MSISDN_1);
        Allure.parameter("Target MSISDN", otherPersonMsisdn);
        Allure.addAttachment("Тело запроса на смену тарифа (попытка)", "application/json", requestBody.toString(), ".json");

        Allure.step("Шаг 2: Отправка PUT запроса на " + PERSON_API_BASE_PATH + "/tariff");
        given()
                .filter(new AllureRestAssured())
                .spec(BASE_JSON_REQUEST_SPEC)
                .header("Authorization", "Bearer " + actingSubscriberToken)
                .body(requestBody)
                .when()
                .put(PERSON_API_BASE_PATH + "/tariff")
                .then()
                .log().all()
                .spec(errorResponseSpec(HTTP_FORBIDDEN));
        Allure.step("Шаг 3: Проверка ответа о запрете (код 403)");
    }

    @Test
    @Tag("brt")
    @Tag("person")
    @DisplayName("BRT-PERSON-006: Администратор успешно пополняет баланс абоненту")
    @Description("Проверка возможности пополнения баланса абонента администратором. Ожидается код 200.")
    @Severity(SeverityLevel.CRITICAL)
    @Story("Пополнение баланса")
    public void adminCanReplenishPersonBalanceSuccessfully() {
        String adminToken = getAdminTokenStep();
        String targetMsisdn = PREDEFINED_SUBSCRIBER_MSISDN_1;
        long amountToReplenish = 504L;

        Allure.step("Шаг 1: Подготовка данных для пополнения баланса (MSISDN: " + targetMsisdn + ", Сумма: " + amountToReplenish + ")");
        Allure.parameter("Target MSISDN", targetMsisdn);
        Allure.parameter("Amount", amountToReplenish);

        Allure.step("Шаг 2: Отправка PUT запроса на " + PERSON_API_BASE_PATH + "/{msisdn}/balance");
        given()
                .filter(new AllureRestAssured())
                .spec(BASE_JSON_REQUEST_SPEC)
                .header("Authorization", "Bearer " + adminToken)
                .pathParam("msisdn", targetMsisdn)
                .queryParam("money", amountToReplenish)
                .when()
                .put(PERSON_API_BASE_PATH + "/{msisdn}/balance")
                .then()
                .log().all()
                .statusCode(HTTP_OK);
        Allure.step("Шаг 3: Проверка успешного ответа (код 200)");
    }

    @Test
    @Tag("brt")
    @Tag("person")
    @DisplayName("BRT-PERSON-007: Абонент успешно пополняет свой собственный баланс")
    @Description("Проверка возможности пополнения собственного баланса абонентом. Ожидается код 200.")
    @Severity(SeverityLevel.CRITICAL)
    @Story("Пополнение баланса")
    public void subscriberCanReplenishOwnBalanceSuccessfully() {
        String subscriberToken = getSubscriberTokenStep(PREDEFINED_SUBSCRIBER_MSISDN_2, PREDEFINED_SUBSCRIBER_PASSWORD_2);
        String ownMsisdn = PREDEFINED_SUBSCRIBER_MSISDN_2;
        long amountToReplenish = 779L;

        Allure.step("Шаг 1: Подготовка данных для пополнения собственного баланса (MSISDN: " + ownMsisdn + ", Сумма: " + amountToReplenish + ")");
        Allure.parameter("Own MSISDN", ownMsisdn);
        Allure.parameter("Amount", amountToReplenish);

        Allure.step("Шаг 2: Отправка PUT запроса на " + PERSON_API_BASE_PATH + "/{msisdn}/balance от имени абонента");
        given()
                .filter(new AllureRestAssured())
                .spec(BASE_JSON_REQUEST_SPEC)
                .header("Authorization", "Bearer " + subscriberToken)
                .pathParam("msisdn", ownMsisdn)
                .queryParam("money", amountToReplenish)
                .when()
                .put(PERSON_API_BASE_PATH + "/{msisdn}/balance")
                .then()
                .log().all()
                .statusCode(HTTP_OK);
        Allure.step("Шаг 3: Проверка успешного ответа (код 200)");
    }

    @Test
    @Tag("brt")
    @Tag("person")
    @DisplayName("BRT-PERSON-008: Абонент не может пополнить баланс другому абоненту (403 Forbidden)")
    @Description("Проверка запрета на пополнение баланса другого абонента пользователем с ролью 'абонент'. Ожидается код 403.")
    @Severity(SeverityLevel.NORMAL)
    @Story("Пополнение баланса (негативные сценарии)")
    public void subscriberCannotReplenishBalanceForAnotherPerson() {
        String actingSubscriberToken = getSubscriberTokenStep(PREDEFINED_SUBSCRIBER_MSISDN_1, PREDEFINED_SUBSCRIBER_PASSWORD_1);
        String otherPersonMsisdn = PREDEFINED_SUBSCRIBER_MSISDN_2; // Используем другого предопределенного абонента
        long amountToReplenish = 75L;

        Allure.step("Шаг 1: Подготовка данных для попытки пополнения баланса другому абоненту (Target MSISDN: " + otherPersonMsisdn + ")");
        Allure.parameter("Acting Subscriber MSISDN", PREDEFINED_SUBSCRIBER_MSISDN_1);
        Allure.parameter("Target MSISDN", otherPersonMsisdn);
        Allure.parameter("Amount", amountToReplenish);

        Allure.step("Шаг 2: Отправка PUT запроса на " + PERSON_API_BASE_PATH + "/{msisdn}/balance");
        given()
                .filter(new AllureRestAssured())
                .spec(BASE_JSON_REQUEST_SPEC)
                .header("Authorization", "Bearer " + actingSubscriberToken)
                .pathParam("msisdn", otherPersonMsisdn)
                .queryParam("money", amountToReplenish)
                .when()
                .put(PERSON_API_BASE_PATH + "/{msisdn}/balance")
                .then()
                .log().all()
                .spec(errorResponseSpec(HTTP_FORBIDDEN));
        Allure.step("Шаг 3: Проверка ответа о запрете (код 403)");
    }

    @Test
    @Tag("brt")
    @Tag("person")
    @DisplayName("BRT-PERSON-009: Администратор успешно получает информацию об абоненте")
    @Description("Проверка получения информации о существующем абоненте администратором. Ожидается код 200 и корректное тело ответа.")
    @Severity(SeverityLevel.NORMAL)
    @Story("Получение информации об абоненте")
    public void adminCanGetPersonInfoSuccessfully() {
        String adminToken = getAdminTokenStep();
        String targetMsisdn = PREDEFINED_SUBSCRIBER_MSISDN_1;

        Allure.step("Шаг 1: Подготовка данных для запроса информации об абоненте (MSISDN: " + targetMsisdn + ")");
        Allure.parameter("Target MSISDN", targetMsisdn);

        Allure.step("Шаг 2: Отправка GET запроса на " + PERSON_API_BASE_PATH + "/{msisdn}");
        given()
                .filter(new AllureRestAssured())
                .spec(BASE_JSON_REQUEST_SPEC)
                .header("Authorization", "Bearer " + adminToken)
                .pathParam("msisdn", targetMsisdn)
                .when()
                .get(PERSON_API_BASE_PATH + "/{msisdn}")
                .then()
                .log().all()
                .statusCode(HTTP_OK)
                .body("msisdn", equalTo(targetMsisdn))
                .body("name", notNullValue())
                .body("balance", notNullValue())
                .body("isRestricted", notNullValue())
                .body("tariff.tariffId", notNullValue());
        Allure.step("Шаг 3: Проверка успешного ответа (код 200 и тело ответа)");
    }

    @Test
    @Tag("brt")
    @Tag("person")
    @DisplayName("BRT-PERSON-010: Абонент успешно получает информацию о себе")
    @Description("Проверка получения информации о себе абонентом. Ожидается код 200 и корректное тело ответа.")
    @Severity(SeverityLevel.NORMAL)
    @Story("Получение информации об абоненте")
    public void subscriberCanGetOwnInfoSuccessfully() {
        String subscriberToken = getSubscriberTokenStep(PREDEFINED_SUBSCRIBER_MSISDN_1, PREDEFINED_SUBSCRIBER_PASSWORD_1);
        String ownMsisdn = PREDEFINED_SUBSCRIBER_MSISDN_1;

        Allure.step("Шаг 1: Подготовка данных для запроса информации о себе (MSISDN: " + ownMsisdn + ")");
        Allure.parameter("Own MSISDN", ownMsisdn);

        Allure.step("Шаг 2: Отправка GET запроса на " + PERSON_API_BASE_PATH + "/{msisdn} от имени абонента");
        given()
                .filter(new AllureRestAssured())
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
        Allure.step("Шаг 3: Проверка успешного ответа (код 200 и тело ответа)");
    }

    @Test
    @Tag("brt")
    @Tag("person")
    @DisplayName("BRT-PERSON-011: Абонент не может получить информацию о другом абоненте (403 Forbidden)")
    @Description("Проверка запрета на получение информации о другом абоненте пользователем с ролью 'абонент'. Ожидается код 403.")
    @Severity(SeverityLevel.NORMAL)
    @Story("Получение информации об абоненте (негативные сценарии)")
    public void subscriberCannotGetInfoForAnotherPerson() {
        String actingSubscriberToken = getSubscriberTokenStep(PREDEFINED_SUBSCRIBER_MSISDN_1, PREDEFINED_SUBSCRIBER_PASSWORD_1);
        String otherPersonMsisdn = PREDEFINED_SUBSCRIBER_MSISDN_2; // Используем другого предопределенного абонента

        Allure.step("Шаг 1: Подготовка данных для попытки запроса информации о другом абоненте (Target MSISDN: " + otherPersonMsisdn + ")");
        Allure.parameter("Acting Subscriber MSISDN", PREDEFINED_SUBSCRIBER_MSISDN_1);
        Allure.parameter("Target MSISDN", otherPersonMsisdn);

        Allure.step("Шаг 2: Отправка GET запроса на " + PERSON_API_BASE_PATH + "/{msisdn}");
        given()
                .filter(new AllureRestAssured())
                .spec(BASE_JSON_REQUEST_SPEC)
                .header("Authorization", "Bearer " + actingSubscriberToken)
                .pathParam("msisdn", otherPersonMsisdn)
                .when()
                .get(PERSON_API_BASE_PATH + "/{msisdn}")
                .then()
                .log().all()
                .spec(errorResponseSpec(HTTP_FORBIDDEN));
        Allure.step("Шаг 3: Проверка ответа о запрете (код 403)");
    }

    @Test
    @Tag("brt")
    @Tag("person")
    @DisplayName("BRT-PERSON-012: Попытка получить информацию о несуществующем абоненте (404 Not Found)")
    @Description("Проверка получения ошибки при запросе информации о несуществующем абоненте. Ожидается код 404.")
    @Severity(SeverityLevel.NORMAL)
    @Story("Получение информации об абоненте (негативные сценарии)")
    public void getInfoForNonExistentPersonReturnsNotFound() {
        String adminToken = getAdminTokenStep();
        String nonExistentMsisdn = TestDataGenerator.generateUniqueMsisdn();

        Allure.step("Шаг 1: Подготовка данных для запроса информации о несуществующем абоненте (MSISDN: " + nonExistentMsisdn + ")");
        Allure.parameter("Non-existent MSISDN", nonExistentMsisdn);

        Allure.step("Шаг 2: Отправка GET запроса на " + PERSON_API_BASE_PATH + "/{msisdn}");
        given()
                .filter(new AllureRestAssured())
                .spec(BASE_JSON_REQUEST_SPEC)
                .header("Authorization", "Bearer " + adminToken)
                .pathParam("msisdn", nonExistentMsisdn)
                .when()
                .get(PERSON_API_BASE_PATH + "/{msisdn}")
                .then()
                .log().all()
                .spec(errorResponseSpec(HTTP_NOT_FOUND));
        Allure.step("Шаг 3: Проверка ответа об ошибке 'Не найдено' (код 404)");
    }
}
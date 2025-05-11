package org.example.auth;

import io.qameta.allure.*;
import io.qameta.allure.restassured.AllureRestAssured;
import org.example.helpers.BaseApiTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static io.restassured.RestAssured.given;
import static org.example.config.ApiTestConfig.*;
import static org.example.helpers.TestDataGenerator.*;

@Epic("Аутентификация (Auth Service)")
@Feature("Регистрация пользователя (Register)")
public class RegisterTests extends BaseApiTest {
    @Test
    @Tag("auth")
    @DisplayName("ASR-001: Успешная регистрация нового администратора")
    @Description("Проверка возможности успешной регистрации нового пользователя с ролью администратора.")
    @Severity(SeverityLevel.CRITICAL)
    @Story("Успешная регистрация")
    public void registerNewAdminSuccess() {
        String uniqueUsername = generateUniqueUsername("new_admin_");
        String password = generateRandomPassword();
        Map<String, Object> requestBody = createAdminRegistrationBody(uniqueUsername, password, ROLE_ADMIN);

        Allure.step("Шаг 1: Формирование тела запроса для регистрации нового администратора: " + uniqueUsername);
        Allure.addAttachment("Тело запроса (New Admin)", "application/json", requestBody.toString(), ".json");
        Allure.parameter("Username", uniqueUsername);
        Allure.parameter("Role", ROLE_ADMIN);

        Allure.step("Шаг 2: Отправка POST запроса на " + REGISTER_PATH);
        given().
                filter(new AllureRestAssured()).
                spec(BASE_JSON_REQUEST_SPEC).
                body(requestBody).
                when().
                post(REGISTER_PATH).
                then().
                log().all().
                spec(SUCCESS_AUTH_RESPONSE_SPEC); // Ожидаем успешный ответ с токенами

        Allure.step("Шаг 3: Проверка успешного ответа (статус 200 OK и наличие токенов)");
    }

    @Test
    @Tag("auth")
    @DisplayName("ASR-002: Успешная регистрация нового абонента (только username, без msisdn)")
    @Description("Проверка регистрации нового пользователя с ролью абонента только по username (без MSISDN). " +
            "Ожидается ошибка, так как для абонента MSISDN должен быть обязателен и известен BRT, ")
    @Severity(SeverityLevel.NORMAL)
    @Story("Неуспешная регистрация (ошибки валидации)")
    public void registerNewSubscriberUsernameOnlyBad() {
        String uniqueUsername = generateUniqueUsername("new_sub_user_");
        String password = generateRandomPassword();
        Map<String, Object> requestBody = createSubscriberRegistrationBodyWithUsernameOnly(uniqueUsername, password, ROLE_SUBSCRIBER);

        Allure.step("Шаг 1: Формирование тела запроса для регистрации абонента только по username: " + uniqueUsername);
        Allure.addAttachment("Тело запроса (Subscriber Username Only)", "application/json", requestBody.toString(), ".json");
        Allure.parameter("Username", uniqueUsername);
        Allure.parameter("Role", ROLE_SUBSCRIBER);

        Allure.step("Шаг 2: Отправка POST запроса на " + REGISTER_PATH);
        given().
                filter(new AllureRestAssured()).
                spec(BASE_JSON_REQUEST_SPEC).
                body(requestBody).
                when().
                post(REGISTER_PATH).
                then().
                log().all().
                spec(errorResponseSpec(HTTP_BAD_REQUEST));

        Allure.step("Шаг 3: Проверка ответа об ошибке (статус " + HTTP_BAD_REQUEST + ")");
    }


    @Test
    @Tag("auth")
    @DisplayName("ASR-003: Неуспешная регистрация (username уже существует)")
    @Description("Проверка невозможности регистрации пользователя с уже существующим username.")
    @Severity(SeverityLevel.CRITICAL)
    @Story("Неуспешная регистрация (конфликты данных)")
    public void registerUserWithExistingUsername() {
        String password = generateRandomPassword();
        Map<String, Object> requestBody = createAdminRegistrationBody(PREDEFINED_ADMIN_USERNAME, password, ROLE_ADMIN);

        Allure.step("Шаг 1: Формирование тела запроса с существующим username: " + PREDEFINED_ADMIN_USERNAME);
        Allure.addAttachment("Тело запроса (Existing Username)", "application/json", requestBody.toString(), ".json");
        Allure.parameter("Username", PREDEFINED_ADMIN_USERNAME);

        Allure.step("Шаг 2: Отправка POST запроса на " + REGISTER_PATH);
        given().
                filter(new AllureRestAssured()).
                spec(BASE_JSON_REQUEST_SPEC).
                body(requestBody).
                when().
                post(REGISTER_PATH).
                then().
                log().all().
                spec(errorResponseSpec(HTTP_BAD_REQUEST));

        Allure.step("Шаг 3: Проверка ответа об ошибке (статус " + HTTP_BAD_REQUEST + ")");
    }

    @Test
    @Tag("auth")
    @DisplayName("ASR-004: Неуспешная регистрация (MSISDN уже существует)")
    @Description("Проверка невозможности регистрации пользователя с MSISDN, который уже занят.")
    @Severity(SeverityLevel.CRITICAL)
    @Story("Неуспешная регистрация (конфликты данных)")
    public void registerUserWithExistingMsisdn() {
        String uniqueUsername = generateUniqueUsername("exist_msisdn_user_");
        String password = generateRandomPassword();
        Map<String, Object> requestBody = createSubscriberRegistrationBodyWithMsisdn(
                uniqueUsername,
                PREDEFINED_SUBSCRIBER_MSISDN_1, // Существующий MSISDN
                password,
                ROLE_SUBSCRIBER
        );

        Allure.step("Шаг 1: Формирование тела запроса с существующим MSISDN: " + PREDEFINED_SUBSCRIBER_MSISDN_1);
        Allure.addAttachment("Тело запроса (Existing MSISDN)", "application/json", requestBody.toString(), ".json");
        Allure.parameter("Username", uniqueUsername);
        Allure.parameter("MSISDN", PREDEFINED_SUBSCRIBER_MSISDN_1);
        Allure.parameter("Role", ROLE_SUBSCRIBER);

        Allure.step("Шаг 2: Отправка POST запроса на " + REGISTER_PATH);
        given().
                filter(new AllureRestAssured()).
                spec(BASE_JSON_REQUEST_SPEC).
                body(requestBody).
                when().
                post(REGISTER_PATH).
                then().
                log().all().
                spec(errorResponseSpec(HTTP_BAD_REQUEST));

        Allure.step("Шаг 3: Проверка ответа об ошибке (статус " + HTTP_BAD_REQUEST + ")");
    }

    @Test
    @Tag("auth")
    @DisplayName("ASR-005: Неуспешная регистрация абонента (MSISDN не известен BRT)")
    @Description("Проверка невозможности регистрации абонента, если его MSISDN не зарегистрирован в BRT (биллинговой системе).")
    @Severity(SeverityLevel.CRITICAL)
    @Story("Неуспешная регистрация (внешние зависимости)")
    public void registerSubscriberWithMsisdnNotKnownToBrtFails() {
        String uniqueUsername = generateUniqueUsername("brt_fail_user_");
        String msisdnNotInBrt = generateUniqueMsisdn(); // Генерируем уникальный, который точно не в BRT
        String password = generateRandomPassword();
        Map<String, Object> requestBody = createSubscriberRegistrationBodyWithMsisdn(
                uniqueUsername,
                msisdnNotInBrt,
                password,
                ROLE_SUBSCRIBER
        );

        Allure.step("Шаг 1: Формирование тела запроса с MSISDN, неизвестным BRT: " + msisdnNotInBrt);
        Allure.addAttachment("Тело запроса (MSISDN not in BRT)", "application/json", requestBody.toString(), ".json");
        Allure.parameter("Username", uniqueUsername);
        Allure.parameter("MSISDN", msisdnNotInBrt);
        Allure.parameter("Role", ROLE_SUBSCRIBER);

        Allure.step("Шаг 2: Отправка POST запроса на " + REGISTER_PATH);
        given().
                filter(new AllureRestAssured()).
                spec(BASE_JSON_REQUEST_SPEC).
                body(requestBody).
                when().
                post(REGISTER_PATH).
                then().
                log().all().
                spec(errorResponseSpec(HTTP_BAD_REQUEST));

        Allure.step("Шаг 3: Проверка ответа об ошибке (статус " + HTTP_BAD_REQUEST + ")");
    }

    @Test
    @Tag("auth")
    @DisplayName("ASR-006: Неуспешная регистрация (не указаны ни username, ни msisdn)")
    @Description("Проверка невозможности регистрации, если не предоставлен ни username, ни MSISDN.")
    @Severity(SeverityLevel.NORMAL)
    @Story("Неуспешная регистрация (ошибки валидации)")
    public void registerUserWithNoLoginIdentifier() {
        String password = generateRandomPassword();
        Map<String, Object> requestBody = createEmptyLoginDetailsRegistrationBody(password, ROLE_SUBSCRIBER);

        Allure.step("Шаг 1: Формирование тела запроса без username и MSISDN");
        Allure.addAttachment("Тело запроса (No Login Identifier)", "application/json", requestBody.toString(), ".json");
        Allure.parameter("Role", ROLE_SUBSCRIBER);

        Allure.step("Шаг 2: Отправка POST запроса на " + REGISTER_PATH);
        given().
                filter(new AllureRestAssured()).
                spec(BASE_JSON_REQUEST_SPEC).
                body(requestBody).
                when().
                post(REGISTER_PATH).
                then().
                log().all().
                spec(errorResponseSpec(HTTP_BAD_REQUEST)); // Ожидаем ошибку 400

        Allure.step("Шаг 3: Проверка ответа об ошибке (статус " + HTTP_BAD_REQUEST + ")");
    }
}
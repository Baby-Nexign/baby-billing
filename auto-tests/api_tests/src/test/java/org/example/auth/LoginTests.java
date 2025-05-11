package org.example.auth;

import io.qameta.allure.*; // Основные аннотации Allure
import io.qameta.allure.restassured.AllureRestAssured; // Для интеграции с RestAssured
import org.example.helpers.BaseApiTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.UUID;

import static io.restassured.RestAssured.given;
import static org.example.config.ApiTestConfig.*;
import static org.example.helpers.TestDataGenerator.*;

@Epic("Аутентификация (Auth Service)")
@Feature("Вход пользователя (Login)")
public class LoginTests extends BaseApiTest {

    @Test
    @Tag("auth")
    @DisplayName("ASL-001: Успешный логин администратора")
    @Description("Проверка успешного входа в систему с учетными данными администратора.")
    @Severity(SeverityLevel.BLOCKER)
    @Story("Успешный логин")
    public void adminLoginSuccess() {
        Map<String, String> requestBody = createLoginRequestBody(PREDEFINED_ADMIN_USERNAME, PREDEFINED_ADMIN_PASSWORD);

        Allure.step("Шаг 1: Формирование тела запроса для логина администратора");
        // Используем Allure.addAttachment для указания типа контента
        Allure.addAttachment("Тело запроса (Admin Login)", "application/json", requestBody.toString(), ".json");

        Allure.step("Шаг 2: Отправка POST запроса на " + LOGIN_PATH);
        given().
                filter(new AllureRestAssured()). // Интеграция Allure с RestAssured
                spec(BASE_JSON_REQUEST_SPEC).
                body(requestBody).
                when().
                post(LOGIN_PATH).
                then().
                log().all(). // Логирование в консоль для отладки (AllureRestAssured логирует в отчет)
                spec(SUCCESS_AUTH_RESPONSE_SPEC);

        Allure.step("Шаг 3: Проверка успешного ответа (статус 200 OK и наличие токенов)");
    }

    @Test
    @Tag("auth")
    @DisplayName("ASL-002: Успешный логин существующего абонента")
    @Description("Проверка успешного входа в систему с учетными данными существующего абонента.")
    @Severity(SeverityLevel.BLOCKER)
    @Story("Успешный логин")
    public void subscriberLoginSuccess() {
        Map<String, String> requestBody = createLoginRequestBody(PREDEFINED_SUBSCRIBER_MSISDN_1, PREDEFINED_SUBSCRIBER_PASSWORD_1);

        Allure.step("Шаг 1: Формирование тела запроса для логина абонента");
        // Используем Allure.addAttachment
        Allure.addAttachment("Тело запроса (Subscriber Login)", "application/json", requestBody.toString(), ".json");

        Allure.step("Шаг 2: Отправка POST запроса на " + LOGIN_PATH);
        given().
                filter(new AllureRestAssured()).
                spec(BASE_JSON_REQUEST_SPEC).
                body(requestBody).
                when().
                post(LOGIN_PATH).
                then().
                log().all().
                spec(SUCCESS_AUTH_RESPONSE_SPEC);

        Allure.step("Шаг 3: Проверка успешного ответа (статус 200 OK и наличие токенов)");
    }

    @Test
    @Tag("auth")
    @DisplayName("ASL-003: Неуспешный логин (неверный пароль)")
    @Description("Проверка поведения системы при попытке входа с верным логином, но неверным паролем.")
    @Severity(SeverityLevel.CRITICAL)
    @Story("Неуспешный логин (ошибки)")
    public void loginWithInvalidPassword() {
        String invalidPassword = "wrongpassword" + UUID.randomUUID().toString().substring(0,8);
        Map<String, String> requestBody = createLoginRequestBody(PREDEFINED_ADMIN_USERNAME, invalidPassword);

        Allure.step("Шаг 1: Формирование тела запроса с неверным паролем");
        // Используем Allure.addAttachment
        Allure.addAttachment("Тело запроса (Invalid Password)", "application/json", requestBody.toString(), ".json");
        Allure.parameter("Username", PREDEFINED_ADMIN_USERNAME);
        // Allure.parameter("Password", invalidPassword, Mode.MASKED); // Можно скрыть пароль в отчете

        Allure.step("Шаг 2: Отправка POST запроса на " + LOGIN_PATH);
        given().
                filter(new AllureRestAssured()).
                spec(BASE_JSON_REQUEST_SPEC).
                body(requestBody).
                when().
                post(LOGIN_PATH).
                then().
                log().all().
                spec(errorResponseSpec(HTTP_BAD_REQUEST)); // Предполагаем, что сервис возвращает 400

        Allure.step("Шаг 3: Проверка ответа об ошибке (статус " + HTTP_BAD_REQUEST + ")");
    }

    @Test
    @Tag("auth")
    @DisplayName("ASL-004: Неуспешный логин (несуществующий пользователь)")
    @Description("Проверка поведения системы при попытке входа с несуществующим логином.")
    @Severity(SeverityLevel.CRITICAL)
    @Story("Неуспешный логин (ошибки)")
    public void loginWithNonExistentUser() {
        String nonExistentUsername = generateUniqueUsername("nonexistent_");
        String randomPassword = generateRandomPassword();
        Map<String, String> requestBody = createLoginRequestBody(nonExistentUsername, randomPassword);

        Allure.step("Шаг 1: Формирование тела запроса с несуществующим пользователем");
        // Используем Allure.addAttachment
        Allure.addAttachment("Тело запроса (Non-existent User)", "application/json", requestBody.toString(), ".json");
        Allure.parameter("Username", nonExistentUsername);

        Allure.step("Шаг 2: Отправка POST запроса на " + LOGIN_PATH);
        given().
                filter(new AllureRestAssured()).
                spec(BASE_JSON_REQUEST_SPEC).
                body(requestBody).
                when().
                post(LOGIN_PATH).
                then().
                log().all().
                spec(errorResponseSpec(HTTP_BAD_REQUEST)); // Предполагаем, что сервис возвращает 400

        Allure.step("Шаг 3: Проверка ответа об ошибке (статус " + HTTP_BAD_REQUEST + ")");
    }
}
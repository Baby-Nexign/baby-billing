package org.example.auth;

import org.example.helpers.BaseApiTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.UUID;

import static io.restassured.RestAssured.given;
import static org.example.config.ApiTestConfig.*;
import static org.example.helpers.TestDataGenerator.*;


public class LoginTests extends BaseApiTest {
    @Test
    @Tag("auth")
    @DisplayName("ASL-001: Успешный логин администратора")
    public void adminLoginSuccess() {
        Map<String, String> requestBody = createLoginRequestBody(PREDEFINED_ADMIN_USERNAME, PREDEFINED_ADMIN_PASSWORD);

        given().
                spec(BASE_JSON_REQUEST_SPEC).
                body(requestBody).
                when().
                post(LOGIN_PATH).
                then().
                log().all().
                spec(SUCCESS_AUTH_RESPONSE_SPEC);
    }

    @Test
    @Tag("auth")
    @DisplayName("ASL-002: Успешный логин существующего абонента")
    public void subscriberLoginSuccess() {
        Map<String, String> requestBody = createLoginRequestBody(PREDEFINED_SUBSCRIBER_MSISDN_1, PREDEFINED_SUBSCRIBER_PASSWORD_1);

        given().
                spec(BASE_JSON_REQUEST_SPEC).
                body(requestBody).
                when().
                post(LOGIN_PATH).
                then().
                log().all().
                spec(SUCCESS_AUTH_RESPONSE_SPEC);
    }

    @Test
    @Tag("auth")
    @DisplayName("ASL-003: Неуспешный логин (неверный пароль)")
    public void loginWithInvalidPassword() {
        Map<String, String> requestBody = createLoginRequestBody(PREDEFINED_ADMIN_USERNAME, "wrongpassword" + UUID.randomUUID().toString());

        given().
                spec(BASE_JSON_REQUEST_SPEC).
                body(requestBody).
                when().
                post(LOGIN_PATH).
                then().
                log().all().
                spec(errorResponseSpec(HTTP_BAD_REQUEST));
    }

    @Test
    @Tag("auth")
    @DisplayName("ASL-004: Неуспешный логин (несуществующий пользователь)")
    public void loginWithNonExistentUser() {
        Map<String, String> requestBody = createLoginRequestBody(generateUniqueUsername("nonexistent_"), generateRandomPassword());

        given().
                spec(BASE_JSON_REQUEST_SPEC).
                body(requestBody).
                when().
                post(LOGIN_PATH).
                then().
                log().all().
                spec(errorResponseSpec(HTTP_BAD_REQUEST));
    }
}
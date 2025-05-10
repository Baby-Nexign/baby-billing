package org.example.auth;

import org.example.helpers.BaseApiTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static io.restassured.RestAssured.given;
import static org.example.config.ApiTestConfig.*;
import static org.example.helpers.TestDataGenerator.*;


public class RegisterTests extends BaseApiTest {

    @Test
    @Tag("auth")
    @DisplayName("ASR-001: Успешная регистрация нового администратора")
    public void registerNewAdminSuccess() {
        String uniqueUsername = generateUniqueUsername("new_admin_");
        String password = generateRandomPassword();
        Map<String, Object> requestBody = createAdminRegistrationBody(uniqueUsername, password, ROLE_ADMIN);

        given().
                spec(BASE_JSON_REQUEST_SPEC).
                body(requestBody).
                when().
                post(REGISTER_PATH).
                then().
                log().all().
                spec(SUCCESS_AUTH_RESPONSE_SPEC);
    }

    @Test
    @Tag("auth")
    @DisplayName("ASR-002: Успешная регистрация нового абонента (только username, без msisdn)")
    public void registerNewSubscriberUsernameOnlySuccess() {
        String uniqueUsername = generateUniqueUsername("new_sub_user_");
        String password = generateRandomPassword();
        Map<String, Object> requestBody = createSubscriberRegistrationBodyWithUsernameOnly(uniqueUsername, password, ROLE_SUBSCRIBER);
        given().
                spec(BASE_JSON_REQUEST_SPEC).
                body(requestBody).
                when().
                post(REGISTER_PATH).
                then().
                log().all().
                spec(errorResponseSpec(HTTP_BAD_REQUEST));
    }


    @Test
    @Tag("auth")
    @DisplayName("ASR-003: Неуспешная регистрация (username уже существует)")
    public void registerUserWithExistingUsername() {
        Map<String, Object> requestBody = createAdminRegistrationBody(PREDEFINED_ADMIN_USERNAME, generateRandomPassword(), ROLE_ADMIN);

        given().
                spec(BASE_JSON_REQUEST_SPEC).
                body(requestBody).
                when().
                post(REGISTER_PATH).
                then().
                log().all().
                spec(errorResponseSpec(HTTP_BAD_REQUEST));
    }

    @Test
    @Tag("auth")
    @DisplayName("ASR-004: Неуспешная регистрация (MSISDN уже существует)")
    public void registerUserWithExistingMsisdn() {
        String uniqueUsername = generateUniqueUsername("exist_msisdn_user_");
        Map<String, Object> requestBody = createSubscriberRegistrationBodyWithMsisdn(
                uniqueUsername,
                PREDEFINED_SUBSCRIBER_MSISDN_1,
                generateRandomPassword(),
                ROLE_SUBSCRIBER
        );

        given().
                spec(BASE_JSON_REQUEST_SPEC).
                body(requestBody).
                when().
                post(REGISTER_PATH).
                then().
                log().all().
                spec(errorResponseSpec(HTTP_BAD_REQUEST));
    }

    @Test
    @Tag("auth")
    @DisplayName("ASR-005: Неуспешная регистрация абонента (MSISDN не известен BRT)")
    public void registerSubscriberWithMsisdnNotKnownToBrtFails() {
        String uniqueUsername = generateUniqueUsername("brt_fail_user_");
        String msisdnNotInBrt = generateUniqueMsisdn();
        Map<String, Object> requestBody = createSubscriberRegistrationBodyWithMsisdn(
                uniqueUsername,
                msisdnNotInBrt,
                generateRandomPassword(),
                ROLE_SUBSCRIBER
        );

        given().
                spec(BASE_JSON_REQUEST_SPEC).
                body(requestBody).
                when().
                post(REGISTER_PATH).
                then().
                log().all().
                spec(errorResponseSpec(HTTP_BAD_REQUEST));
    }

    @Test
    @Tag("auth")
    @DisplayName("ASR-006: Неуспешная регистрация (не указаны ни username, ни msisdn)")
    public void registerUserWithNoLoginIdentifier() {
        Map<String, Object> requestBody = createEmptyLoginDetailsRegistrationBody(generateRandomPassword(), ROLE_SUBSCRIBER);

        given().
                spec(BASE_JSON_REQUEST_SPEC).
                body(requestBody).
                when().
                post(REGISTER_PATH).
                then().
                log().all().
                spec(errorResponseSpec(HTTP_BAD_REQUEST));
    }
}
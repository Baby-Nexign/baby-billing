package org.example.helpers;

import io.restassured.response.Response;

import java.util.Map;

import static io.restassured.RestAssured.given;
import static org.example.config.ApiTestConfig.*;
import static org.example.helpers.TestDataGenerator.createLoginRequestBody;

public class AuthHelper {
    public static String getAdminToken() {
        Map<String, String> requestBody = createLoginRequestBody(
                PREDEFINED_ADMIN_USERNAME,
                PREDEFINED_ADMIN_PASSWORD
        );

        Response response = given()
                .spec(BASE_JSON_REQUEST_SPEC)
                .body(requestBody)
                .when()
                .post(LOGIN_PATH)
                .then()
                .log().ifError()
                .spec(SUCCESS_AUTH_RESPONSE_SPEC)
                .extract()
                .response();

        return response.jsonPath().getString("accessToken");
    }

    public static String getSubscriberToken() {
        return getSubscriberToken(PREDEFINED_SUBSCRIBER_MSISDN_1, PREDEFINED_SUBSCRIBER_PASSWORD_1);
    }

    public static String getSubscriberToken(String login, String password) {
        Map<String, String> requestBody = createLoginRequestBody(login, password);

        Response response = given()
                .spec(BASE_JSON_REQUEST_SPEC)
                .body(requestBody)
                .when()
                .post(LOGIN_PATH)
                .then()
                .log().ifError()
                .spec(SUCCESS_AUTH_RESPONSE_SPEC)
                .extract()
                .response();

        return response.jsonPath().getString("accessToken");
    }
}
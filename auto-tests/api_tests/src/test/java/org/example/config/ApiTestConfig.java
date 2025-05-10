package org.example.config;

import io.restassured.builder.RequestSpecBuilder;
import io.restassured.builder.ResponseSpecBuilder;
import io.restassured.http.ContentType;
import io.restassured.specification.RequestSpecification;
import io.restassured.specification.ResponseSpecification;

import static org.hamcrest.Matchers.emptyOrNullString;
import static org.hamcrest.Matchers.not;

public class ApiTestConfig {

    // Базовый URI и пути
    public static final String BASE_URI = "http://localhost:8084/api"; // Через CRM Gateway
    public static final String AUTH_BASE_PATH = "/auth";
    public static final String LOGIN_PATH = AUTH_BASE_PATH + "/login";
    public static final String REGISTER_PATH = AUTH_BASE_PATH + "/register";
    public static final String PERSON_API_BASE_PATH = "/person";


    // Роли
    public static final String ROLE_ADMIN = "ADMIN";
    public static final String ROLE_SUBSCRIBER = "SUBSCRIBER";

    // Данные предустановленных пользователей
    public static final String PREDEFINED_ADMIN_USERNAME = "admin";
    public static final String PREDEFINED_ADMIN_PASSWORD = "admin";
    public static final String PREDEFINED_SUBSCRIBER_MSISDN_1 = "79123456789";
    public static final String PREDEFINED_SUBSCRIBER_PASSWORD_1 = PREDEFINED_SUBSCRIBER_MSISDN_1; // Пароль = MSISDN

    public static final String PREDEFINED_SUBSCRIBER_MSISDN_2 = "79996667755";
    public static final String PREDEFINED_SUBSCRIBER_PASSWORD_2 = PREDEFINED_SUBSCRIBER_MSISDN_2; // Пароль = MSISDN

    private static final String COMMUTATOR_API_PATH = "/commutator";
    public static final String GENERATE_CDR_ENDPOINT = COMMUTATOR_API_PATH + "/generate-cdr";

    // Ожидаемые HTTP статус коды
    public static final int HTTP_OK = 200;
    public static final int HTTP_CREATED = 201; // Часто используется для POST при создании ресурса
    public static final int HTTP_BAD_REQUEST = 400;
    public static final int HTTP_UNAUTHORIZED = 401;
    public static final int HTTP_FORBIDDEN = 403;
    public static final int HTTP_NOT_FOUND = 404;
    public static final int HTTP_CONFLICT = 409;
    public static final int HTTP_INTERNAL_SERVER_ERROR = 500;

    // Общие спецификации RestAssured
    public static final RequestSpecification BASE_JSON_REQUEST_SPEC = new RequestSpecBuilder()
            .setContentType(ContentType.JSON)
            .build();

    public static final ResponseSpecification SUCCESS_AUTH_RESPONSE_SPEC = new ResponseSpecBuilder()
            .expectStatusCode(HTTP_OK)
            .expectContentType(ContentType.JSON)
            .expectBody("accessToken", not(emptyOrNullString()))
            .expectBody("refreshToken", not(emptyOrNullString()))
            .build();

    public static ResponseSpecification errorResponseSpec(int expectedStatusCode) {
        ResponseSpecBuilder specBuilder = new ResponseSpecBuilder();
        specBuilder.expectStatusCode(expectedStatusCode);
        if (expectedStatusCode == HTTP_BAD_REQUEST ||
                expectedStatusCode == HTTP_NOT_FOUND ||
                expectedStatusCode == HTTP_CONFLICT) {
            specBuilder.expectContentType(ContentType.JSON);

        }
        return specBuilder.build();
    }
}
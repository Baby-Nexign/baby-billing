package org.example.cdr;

import org.example.helpers.BaseApiTest;
import org.example.helpers.AuthHelper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.given;
import static org.example.config.ApiTestConfig.*;

public class CommutatorServiceTests extends BaseApiTest {
    @Test
    @Tag("commutator")
    @DisplayName("CDR-001: Администратор успешно инициирует генерацию CDR")
    public void adminCanSuccessfullyGenerateCdr() {
        String adminToken = AuthHelper.getAdminToken();

        given()
                .spec(BASE_JSON_REQUEST_SPEC)
                .header("Authorization", "Bearer " + adminToken)
                .when()
                .post(GENERATE_CDR_ENDPOINT)
                .then()
                .log().all()
                .statusCode(HTTP_OK);
    }

    @Test
    @Tag("commutator")
    @DisplayName("CDR-002: Абонент не может инициировать генерацию CDR (403 Forbidden)")
    public void subscriberCannotGenerateCdr() {
        String subscriberToken = AuthHelper.getSubscriberToken();

        given()
                .spec(BASE_JSON_REQUEST_SPEC)
                .header("Authorization", "Bearer " + subscriberToken)
                .when()
                .post(GENERATE_CDR_ENDPOINT)
                .then()
                .log().all()
                .spec(errorResponseSpec(HTTP_FORBIDDEN));
    }

    @Test
    @Tag("commutator")
    @DisplayName("CDR-003: Не авторизованный юзер не может инициировать генерацию CDR (401 UNAUTHORIZED)")
    public void unauthorizeCannotGenerateCdr() {
        given()
                .spec(BASE_JSON_REQUEST_SPEC)
                .when()
                .post(GENERATE_CDR_ENDPOINT)
                .then()
                .log().all()
                .spec(errorResponseSpec(HTTP_UNAUTHORIZED));
    }
}
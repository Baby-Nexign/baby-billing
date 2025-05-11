package org.example.cdr;

import io.qameta.allure.*;
import io.qameta.allure.restassured.AllureRestAssured;
import org.example.helpers.BaseApiTest;
import org.example.helpers.AuthHelper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.given;
import static org.example.config.ApiTestConfig.*;

@Epic("Commutator Сервис (CDR)")
@Feature("Генерация CDR записей")
public class CommutatorServiceTests extends BaseApiTest {

    @Step("Получение токена администратора")
    private String getAdminTokenStep() {
        String token = AuthHelper.getAdminToken();
         Allure.addAttachment("Admin Token (частично)", "text/plain", token.substring(0, Math.min(token.length(), 20)) + "...", ".txt");
        return token;
    }

    @Step("Получение токена абонента")
    private String getSubscriberTokenStep() {
        String token = AuthHelper.getSubscriberToken();
         Allure.addAttachment("Subscriber Token (частично)", "text/plain", token.substring(0, Math.min(token.length(), 20)) + "...", ".txt");
        return token;
    }

    @Test
    @Tag("commutator")
    @DisplayName("CDR-001: Администратор успешно инициирует генерацию CDR")
    @Description("Проверка, что пользователь с ролью 'администратор' может успешно запустить процесс генерации CDR файлов. Ожидается код 200 OK.")
    @Severity(SeverityLevel.CRITICAL)
    @Story("Успешная генерация CDR")
    public void adminCanSuccessfullyGenerateCdr() {
        String adminToken = getAdminTokenStep();
        Allure.parameter("User Role", "ADMIN");

        Allure.step("Шаг 1: Отправка POST запроса на " + GENERATE_CDR_ENDPOINT + " от имени администратора");
        given()
                .filter(new AllureRestAssured())
                .spec(BASE_JSON_REQUEST_SPEC)
                .header("Authorization", "Bearer " + adminToken)
                .when()
                .post(GENERATE_CDR_ENDPOINT)
                .then()
                .log().all()
                .statusCode(HTTP_OK);
        Allure.step("Шаг 2: Проверка успешного ответа (код 200)");
    }

    @Test
    @Tag("commutator")
    @DisplayName("CDR-002: Абонент не может инициировать генерацию CDR (403 Forbidden)")
    @Description("Проверка, что пользователь с ролью 'абонент' не может запустить процесс генерации CDR файлов. Ожидается код 403 Forbidden.")
    @Severity(SeverityLevel.NORMAL)
    @Story("Генерация CDR (негативные сценарии, авторизация)")
    public void subscriberCannotGenerateCdr() {
        String subscriberToken = getSubscriberTokenStep();
        Allure.parameter("User Role", "SUBSCRIBER");

        Allure.step("Шаг 1: Отправка POST запроса на " + GENERATE_CDR_ENDPOINT + " от имени абонента");
        given()
                .filter(new AllureRestAssured())
                .spec(BASE_JSON_REQUEST_SPEC)
                .header("Authorization", "Bearer " + subscriberToken)
                .when()
                .post(GENERATE_CDR_ENDPOINT)
                .then()
                .log().all()
                .spec(errorResponseSpec(HTTP_FORBIDDEN));
        Allure.step("Шаг 2: Проверка ответа о запрете (код 403)");
    }

    @Test
    @Tag("commutator")
    @DisplayName("CDR-003: Не авторизованный юзер не может инициировать генерацию CDR (401 UNAUTHORIZED)")
    @Description("Проверка, что неавторизованный пользователь не может запустить процесс генерации CDR файлов. Ожидается код 401 Unauthorized.")
    @Severity(SeverityLevel.NORMAL)
    @Story("Генерация CDR (негативные сценарии, аутентификация)")
    public void unauthorizeCannotGenerateCdr() {
        Allure.parameter("User Role", "UNAUTHORIZED");

        Allure.step("Шаг 1: Отправка POST запроса на " + GENERATE_CDR_ENDPOINT + " без токена аутентификации");
        given()
                .filter(new AllureRestAssured())
                .spec(BASE_JSON_REQUEST_SPEC)
                .when()
                .post(GENERATE_CDR_ENDPOINT)
                .then()
                .log().all()
                .spec(errorResponseSpec(HTTP_UNAUTHORIZED)); // Проверка ожидаемого кода ошибки
        Allure.step("Шаг 2: Проверка ответа об отсутствии авторизации (код 401)");
    }
}
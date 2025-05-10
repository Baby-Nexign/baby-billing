package org.example.helpers;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.UUID;

public class TestDataGenerator {

    private static final Random random = new Random();

    public static String generateUniqueUsername(String prefix) {
        return prefix + "_" + UUID.randomUUID().toString().substring(0, 8);
    }

    public static String generateUniqueMsisdn() {
        return "79" + String.format("%09d", random.nextInt(1_000_000_000));
    }

    public static String generateRandomPassword() {
        return "Pass_" + UUID.randomUUID().toString().substring(0, 10);
    }

    // --- Методы для создания тел запросов ---

    public static Map<String, String> createLoginRequestBody(String login, String password) {
        Map<String, String> body = new HashMap<>();
        body.put("login", login);
        body.put("password", password);
        return body;
    }

    public static Map<String, Object> createAdminRegistrationBody(String username, String password, String role) {
        Map<String, Object> body = new HashMap<>();
        body.put("username", username);
        body.put("password", password);
        body.put("roles", List.of(role));
        return body;
    }

    public static Map<String, Object> createSubscriberRegistrationBodyWithUsernameOnly(String username, String password, String role) {
        Map<String, Object> body = new HashMap<>();
        body.put("username", username);
        body.put("password", password);
        body.put("roles", List.of(role));
        return body;
    }

    public static Map<String, Object> createSubscriberRegistrationBodyWithMsisdn(String username, String msisdn, String password, String role) {
        Map<String, Object> body = new HashMap<>();
        if (username != null) { // username может быть опциональным, если есть msisdn
            body.put("username", username);
        }
        body.put("msisdn", msisdn);
        body.put("password", password);
        body.put("roles", List.of(role));
        return body;
    }

    public static Map<String, Object> createEmptyLoginDetailsRegistrationBody(String password, String role) {
        Map<String, Object> body = new HashMap<>();
        body.put("password", password);
        body.put("roles", List.of(role));
        return body;
    }

    public static Map<String, Object> createPersonRequestBody(String name, String msisdn, String description) {
        Map<String, Object> body = new HashMap<>();
        body.put("name", name);
        body.put("msisdn", msisdn);
        if (description != null) {
            body.put("description", description);
        }
        return body;
    }
    public static Map<String, Object> createChangePersonTariffRequestBody(String personMsisdn, Long newTariffId) {
        Map<String, Object> body = new HashMap<>();
        body.put("personMsisdn", personMsisdn);
        body.put("newTariffId", newTariffId);
        return body;
    }

    public static Map<String, Object> createServiceRequestBody(String name, String description) {
        Map<String, Object> body = new HashMap<>();
        if (name != null) {
            body.put("name", name);
        }
        if (description != null) {
            body.put("description", description);
        }
        return body;
    }

    public static Map<String, Object> createTariffRequestBody(String name, String description) {
        Map<String, Object> body = new HashMap<>();
        if (name != null) {
            body.put("name", name);
        }
        if (description != null) {
            body.put("description", description);
        }
        return body;
    }
}
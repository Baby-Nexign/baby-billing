package org.example.helpers;

import io.restassured.RestAssured;
import org.junit.jupiter.api.BeforeEach;

import static org.example.config.ApiTestConfig.BASE_URI;

public abstract class BaseApiTest {

    @BeforeEach
    public void globalSetup() {
        RestAssured.baseURI = BASE_URI;
        // ...
    }
}
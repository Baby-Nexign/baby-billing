package org.babynexign.babybilling.crmservice.config;

import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.function.Predicate;

@Component
public class RouterValidator {
    public static final List<String> openApiEndpoints = List.of(
            "/auth/register",
            "/auth/login",
            "/api/services",
            "/api/services/[0-9]+",
            "/api/tariffs",
            "/api/tariffs/[0-9]+"
    );

    public Predicate<ServerHttpRequest> isSecured =
            request -> openApiEndpoints
                    .stream()
                    .noneMatch(uri ->
                            request.getURI().getPath().matches(".*" + uri + ".*"));
}

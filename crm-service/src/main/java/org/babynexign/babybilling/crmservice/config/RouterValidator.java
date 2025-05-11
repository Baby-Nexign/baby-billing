package org.babynexign.babybilling.crmservice.config;

import lombok.AllArgsConstructor;
import lombok.Data;
import org.springframework.http.HttpMethod;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.function.Predicate;

@Component
public class RouterValidator {
    @Data
    @AllArgsConstructor
    public static class OpenEndpoint {
        private String pathPattern;
        private HttpMethod method;
    }

    public static final List<OpenEndpoint> openApiEndpoints = List.of(
            new OpenEndpoint("/auth/register", null),
            new OpenEndpoint("/auth/login", null),
            new OpenEndpoint("/api/services", HttpMethod.GET),
            new OpenEndpoint("/api/services/[0-9]+", HttpMethod.GET),
            new OpenEndpoint("/api/tariffs", HttpMethod.GET),
            new OpenEndpoint("/api/tariffs/[0-9]+", HttpMethod.GET)
    );

    public Predicate<ServerHttpRequest> isSecured = request -> openApiEndpoints
            .stream()
            .noneMatch(endpoint -> {
                boolean pathMatches = request.getURI().getPath().matches(".*" + endpoint.getPathPattern() + ".*");

                boolean methodMatches = endpoint.getMethod() == null ||
                        endpoint.getMethod().equals(request.getMethod());

                return pathMatches && methodMatches;
            });
}

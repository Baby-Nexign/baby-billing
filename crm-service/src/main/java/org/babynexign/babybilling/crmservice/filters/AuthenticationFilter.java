package org.babynexign.babybilling.crmservice.filters;

import io.jsonwebtoken.Claims;
import lombok.RequiredArgsConstructor;
import org.babynexign.babybilling.crmservice.config.RouterValidator;
import org.babynexign.babybilling.crmservice.util.JwtUtil;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.List;

@Component
@RequiredArgsConstructor
public class AuthenticationFilter implements GatewayFilter {
    private final RouterValidator routerValidator;
    private final JwtUtil jwtUtil;

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();

        if (routerValidator.isSecured.test(request)) {
            if (this.isAuthMissing(request)) {
                return this.onError(exchange, HttpStatus.UNAUTHORIZED);
            }

            final String token = this.getAuthHeader(request);

            if (jwtUtil.isInvalid(token)) {
                return this.onError(exchange, HttpStatus.FORBIDDEN);
            }

            if (!hasAccess(request, token)) {
                return this.onError(exchange, HttpStatus.FORBIDDEN);
            }

            this.updateRequest(exchange, token);
        }

        return chain.filter(exchange);
    }

    private boolean hasAccess(ServerHttpRequest request, String token) {
        String path = request.getURI().getPath();

        if (jwtUtil.hasAdminRole(token)) {
            return true;
        }

        String userMsisdn = jwtUtil.getMsisdnFromToken(token);

        if (userMsisdn == null || userMsisdn.isEmpty()) {
            return false;
        }

        if (path.matches(".*/api/person/[^/]+")) {
            String pathMsisdn = extractMsisdnFromPath(path);
            return pathMsisdn.equals(userMsisdn);
        }

        if (path.matches(".*/api/person/[^/]+/balance")) {
            String pathMsisdn = extractMsisdnFromPath(path);
            return pathMsisdn.equals(userMsisdn);
        }

        return !path.contains("/generate-cdr") &&
                (!path.matches(".*/api/person") || !isPostMethod(request)) &&
                (!path.matches(".*/api/services") || !isPostMethod(request)) &&
                (!path.matches(".*/api/tariffs") || !isPostMethod(request));
    }

    private String extractMsisdnFromPath(String path) {
        String[] parts = path.split("/");
        for (int i = 0; i < parts.length; i++) {
            if (parts[i].equals("person") && i + 1 < parts.length) {
                String msisdn = parts[i + 1];
                if (msisdn.matches("\\d+")) {
                    return msisdn;
                }
            }
        }
        return "";
    }

    private boolean isPostMethod(ServerHttpRequest request) {
        return "POST".equals(request.getMethod().name());
    }

    private Mono<Void> onError(ServerWebExchange exchange, HttpStatus httpStatus) {
        ServerHttpResponse response = exchange.getResponse();
        response.setStatusCode(httpStatus);
        return response.setComplete();
    }

    private String getAuthHeader(ServerHttpRequest request) {
        return request.getHeaders().getOrEmpty("Authorization").get(0);
    }

    private boolean isAuthMissing(ServerHttpRequest request) {
        return !request.getHeaders().containsKey("Authorization");
    }

    private void updateRequest(ServerWebExchange exchange, String token) {
        Claims claims = jwtUtil.getAllClaimsFromToken(token);

        ServerHttpRequest request = exchange.getRequest().mutate()
                .header("X-User-Id", claims.getSubject())
                .build();

        String msisdn = jwtUtil.getMsisdnFromToken(token);
        if (msisdn != null && !msisdn.isEmpty()) {
            request = request.mutate()
                    .header("X-User-Msisdn", msisdn)
                    .build();
        }

        List<String> roles = jwtUtil.getRolesFromToken(token);
        if (roles != null && !roles.isEmpty()) {
            request = request.mutate()
                    .header("X-User-Roles", String.join(",", roles))
                    .build();
        }

        exchange.mutate().request(request).build();
    }
}

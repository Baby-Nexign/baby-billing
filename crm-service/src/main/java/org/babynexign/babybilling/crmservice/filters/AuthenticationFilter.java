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

import java.util.Date;
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

            return chain.filter(updateRequest(exchange, token));
        }

        return chain.filter(exchange);
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

    private ServerWebExchange updateRequest(ServerWebExchange exchange, String token) {
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

        boolean isAdmin = jwtUtil.hasAdminRole(token);
        request = request.mutate()
                .header("X-User-Is-Admin", String.valueOf(isAdmin))
                .build();

        Date expiration = claims.getExpiration();
        if (expiration != null) {
            request = request.mutate()
                    .header("X-Token-Expiration", String.valueOf(expiration.getTime()))
                    .build();
        }

        return exchange.mutate().request(request).build();
    }
}

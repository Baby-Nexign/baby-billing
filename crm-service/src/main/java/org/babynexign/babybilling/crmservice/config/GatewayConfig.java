package org.babynexign.babybilling.crmservice.config;

import org.babynexign.babybilling.crmservice.filters.AuthenticationFilter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class GatewayConfig {
    private final AuthenticationFilter filter;

    @Autowired
    public GatewayConfig(AuthenticationFilter filter) {
        this.filter = filter;
    }

    @Bean
    public RouteLocator routes(RouteLocatorBuilder builder) {
        return builder.routes()
                .route("auth-service", r -> r.path("/api/auth/**")
                        .filters(f -> f.filter(filter))
                        .uri("lb://auth-service"))

                .route("commutator-service", r -> r.path("/api/commutator/**")
                        .filters(f -> f.filter(filter))
                        .uri("lb://commutator-service"))

                .route("brt-service-person", r -> r.path("/api/person/**")
                        .filters(f -> f.filter(filter))
                        .uri("lb://brt-service"))

                .route("hrs-service-services", r -> r.path("/api/services/**")
                        .filters(f -> f.filter(filter))
                        .uri("lb://hrs-service"))
                .route("hrs-service-tariffs", r -> r.path("/api/tariffs/**")
                        .filters(f -> f.filter(filter))
                        .uri("lb://hrs-service"))

                .build();
    }
}

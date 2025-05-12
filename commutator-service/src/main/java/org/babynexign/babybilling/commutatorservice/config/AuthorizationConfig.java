package org.babynexign.babybilling.commutatorservice.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;

@Configuration
public class AuthorizationConfig {

    @Bean
    public OncePerRequestFilter authorizationFilter() {
        return new OncePerRequestFilter() {
            @Override
            protected void doFilterInternal(
                    HttpServletRequest request,
                    HttpServletResponse response,
                    FilterChain filterChain) throws ServletException, IOException {
                
                if (isPublicEndpoint(request.getRequestURI(), request.getMethod())) {
                    filterChain.doFilter(request, response);
                    return;
                }

                if (!hasAccess(request)) {
                    response.setStatus(HttpServletResponse.SC_FORBIDDEN);
                    return;
                }

                filterChain.doFilter(request, response);
            }
        };
    }

    private boolean isPublicEndpoint(String uri, String method) {
        return uri.matches(".*/h2-console.*");
    }

    private boolean hasAccess(HttpServletRequest request) {
        String isAdmin = request.getHeader("X-User-Is-Admin");

        return Boolean.parseBoolean(isAdmin);
    }
}
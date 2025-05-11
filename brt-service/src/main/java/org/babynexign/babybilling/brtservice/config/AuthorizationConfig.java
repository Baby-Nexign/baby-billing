package org.babynexign.babybilling.brtservice.config;

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
        return false;
    }

    private boolean hasAccess(HttpServletRequest request) {
        String userMsisdn = request.getHeader("X-User-Msisdn");
        String isAdmin = request.getHeader("X-User-Is-Admin");
        
        if (Boolean.parseBoolean(isAdmin)) {
            return true;
        }

        String uri = request.getRequestURI();
        String method = request.getMethod();

        if (uri.matches(".*/person/\\d+.*") && userMsisdn != null) {
            String pathMsisdn = extractMsisdnFromPath(uri);
            return pathMsisdn.equals(userMsisdn);
        }

        return !"POST".equals(method);
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
}
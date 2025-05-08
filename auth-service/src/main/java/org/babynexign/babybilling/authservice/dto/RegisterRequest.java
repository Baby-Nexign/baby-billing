package org.babynexign.babybilling.authservice.dto;

import java.util.Set;

public record RegisterRequest(
        String username,
        String msisdn,
        String password,
        Set<String> roles
) {}

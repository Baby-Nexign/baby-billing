package org.babynexign.babybilling.brtservice.exception;

import java.time.LocalDateTime;

public record ErrorResponse(Integer status, String message, LocalDateTime timestamp) {}

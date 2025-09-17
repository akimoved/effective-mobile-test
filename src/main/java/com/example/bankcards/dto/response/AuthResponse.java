package com.example.bankcards.dto.response;

/**
 * DTO для ответа аутентификации
 */
public record AuthResponse(
        String token,
        long expiresIn
) {}

package com.example.bankcards.dto.request;

import jakarta.validation.constraints.NotBlank;

/**
 * DTO для запроса аутентификации
 */
public record LoginRequest(
        @NotBlank(message = "Логин не может быть пустым")
        String login,
        
        @NotBlank(message = "Пароль не может быть пустым")
        String password
) {}

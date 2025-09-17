package com.example.bankcards.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * DTO для запроса регистрации
 */
public record RegisterRequest(
        @NotBlank(message = "Имя пользователя не может быть пустым")
        @Size(min = 3, max = 50, message = "Имя пользователя должно быть от 3 до 50 символов")
        String username,
        
        @NotBlank(message = "Email не может быть пустым")
        @Email(message = "Некорректный формат email")
        @Size(max = 100, message = "Email не должен превышать 100 символов")
        String email,
        
        @NotBlank(message = "Пароль не может быть пустым")
        @Size(min = 6, max = 100, message = "Пароль должен быть от 6 до 100 символов")
        String password,
        
        @NotBlank(message = "Имя не может быть пустым")
        @Size(max = 50, message = "Имя не должно превышать 50 символов")
        String firstName,
        
        @NotBlank(message = "Фамилия не может быть пустой")
        @Size(max = 50, message = "Фамилия не должна превышать 50 символов")
        String lastName
) {}

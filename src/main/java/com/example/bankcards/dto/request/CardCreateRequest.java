package com.example.bankcards.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * DTO для создания новой карты
 */
public record CardCreateRequest(
        @NotBlank(message = "Номер карты не может быть пустым")
        @Pattern(regexp = "\\d{16}", message = "Номер карты должен содержать 16 цифр")
        @Size(min = 16, max = 19)
        String cardNumber,
        
        @NotBlank(message = "Имя держателя карты не может быть пустым")
        @Size(max = 100, message = "Имя держателя карты не должно превышать 100 символов")
        String cardholderName
) {}

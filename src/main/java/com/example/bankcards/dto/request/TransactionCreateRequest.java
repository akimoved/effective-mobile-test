package com.example.bankcards.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

/**
 * DTO для создания перевода между картами
 */
public record TransactionCreateRequest(
        @NotNull(message = "ID карты отправителя не может быть пустым")
        Long fromCardId,
        
        @NotNull(message = "ID карты получателя не может быть пустым")
        Long toCardId,
        
        @NotNull(message = "Сумма перевода не может быть пустой")
        @DecimalMin(value = "0.01", message = "Сумма перевода должна быть больше 0")
        BigDecimal amount,
        
        @Size(max = 500, message = "Описание не должно превышать 500 символов")
        String description
) {}

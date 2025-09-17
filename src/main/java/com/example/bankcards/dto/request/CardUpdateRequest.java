package com.example.bankcards.dto.request;

import com.example.bankcards.entity.CardStatus;
import jakarta.validation.constraints.Size;

/**
 * DTO для обновления карты
 */
public record CardUpdateRequest(
        @Size(max = 100, message = "Имя держателя карты не должно превышать 100 символов")
        String cardholderName,
        
        CardStatus status
) {}

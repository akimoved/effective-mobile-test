package com.example.bankcards.dto.response;

import com.example.bankcards.entity.enums.CardStatus;

import java.time.LocalDateTime;

/**
 * DTO для ответа с данными карты
 */
public record CardResponse(
        Long id,
        String cardNumber,
        String cardholderName,
        CardStatus status,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {}

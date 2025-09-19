package com.example.bankcards.dto.response;

import com.example.bankcards.entity.enums.TransactionStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * DTO для ответа с данными транзакции
 */
public record TransactionResponse(
        Long id,
        Long fromCardId,
        String fromCardNumber,
        Long toCardId,
        String toCardNumber,
        BigDecimal amount,
        String description,
        TransactionStatus status,
        LocalDateTime transactionDate,
        LocalDateTime completedAt,
        String errorMessage
) {}

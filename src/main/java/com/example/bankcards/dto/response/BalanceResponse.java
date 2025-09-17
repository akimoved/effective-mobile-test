package com.example.bankcards.dto.response;

import java.math.BigDecimal;

/**
 * DTO для ответа с балансом карты
 */
public record BalanceResponse(
        Long cardId,
        String maskedCardNumber,
        BigDecimal balance,
        String cardholderName
) {}

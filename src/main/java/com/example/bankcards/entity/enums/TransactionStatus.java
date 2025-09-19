package com.example.bankcards.entity.enums;

import lombok.Getter;

/**
 * Статусы транзакций
 */
@Getter
public enum TransactionStatus {
    PENDING("В обработке"),
    COMPLETED("Завершена"),
    FAILED("Неудачная"),
    CANCELLED("Отменена");

    private final String displayName;

    TransactionStatus(String displayName) {
        this.displayName = displayName;
    }

}

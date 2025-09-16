package com.example.bankcards.entity;

/**
 * Статусы транзакций
 */
public enum TransactionStatus {
    PENDING("В обработке"),
    COMPLETED("Завершена"),
    FAILED("Неудачная"),
    CANCELLED("Отменена");

    private final String displayName;

    TransactionStatus(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}

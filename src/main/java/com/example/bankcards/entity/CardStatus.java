package com.example.bankcards.entity;

/**
 * Статусы банковских карт
 */
public enum CardStatus {
    ACTIVE("Активна"),
    BLOCKED("Заблокирована"),
    EXPIRED("Истек срок");

    private final String displayName;

    CardStatus(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}

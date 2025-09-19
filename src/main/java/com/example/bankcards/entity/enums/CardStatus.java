package com.example.bankcards.entity.enums;

import lombok.Getter;

/**
 * Статусы банковских карт
 */
@Getter
public enum CardStatus {
    ACTIVE("Активна"),
    BLOCKED("Заблокирована"),
    EXPIRED("Истек срок");

    private final String displayName;

    CardStatus(String displayName) {
        this.displayName = displayName;
    }

}

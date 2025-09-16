package com.example.bankcards.entity;

/**
 * Роли пользователей в системе
 */
public enum RoleName {
    ADMIN("Администратор"),
    USER("Пользователь");

    private final String displayName;

    RoleName(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}

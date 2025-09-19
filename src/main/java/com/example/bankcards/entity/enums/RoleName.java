package com.example.bankcards.entity.enums;

import lombok.Getter;

/**
 * Роли пользователей в системе
 */
@Getter
public enum RoleName {
    ROLE_ADMIN("Администратор"),
    ROLE_USER("Пользователь");

    private final String displayName;

    RoleName(String displayName) {
        this.displayName = displayName;
    }

}

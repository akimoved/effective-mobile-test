package com.example.bankcards.exception;

/**
 * Исключение при отсутствии пользователя
 */
public class UserNotFoundException extends RuntimeException {
    public UserNotFoundException(String message) {
        super(message);
    }
}

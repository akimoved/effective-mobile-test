package com.example.bankcards.exception;

/**
 * Исключение при попытке создать пользователя с уже существующим username или email
 */
public class UserAlreadyExistsException extends RuntimeException {
    public UserAlreadyExistsException(String message) {
        super(message);
    }
}

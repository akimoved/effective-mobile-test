package com.example.bankcards.exception;

/**
 * Исключение, выбрасываемое при некорректной транзакции
 */
public class InvalidTransactionException extends RuntimeException {
    public InvalidTransactionException(String message) {
        super(message);
    }
}

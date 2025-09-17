package com.example.bankcards.exception;

/**
 * Исключение, выбрасываемое при отсутствии транзакции
 */
public class TransactionNotFoundException extends RuntimeException {
    public TransactionNotFoundException(String message) {
        super(message);
    }
}

package com.example.bankcards.exception;

/**
 * Исключение, выбрасываемое при недостатке средств на карте
 */
public class InsufficientFundsException extends RuntimeException {
    public InsufficientFundsException(String message) {
        super(message);
    }
}

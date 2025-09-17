package com.example.bankcards.exception;

/**
 * Исключение, выбрасываемое при отсутствии карты
 */
public class CardNotFoundException extends RuntimeException {
    public CardNotFoundException(String message) {
        super(message);
    }
}

package com.example.bankcards.exception;

/**
 * Исключение, выбрасываемое при попытке создать карту с уже существующим номером
 */
public class DuplicateCardNumberException extends RuntimeException {
    public DuplicateCardNumberException(String message) {
        super(message);
    }
}

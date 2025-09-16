package com.example.bankcards.exception;

/**
 * Исключение при ошибках шифрования/дешифрования
 */
public class EncryptionException extends RuntimeException {
    public EncryptionException(String message, Throwable cause) {
        super(message, cause);
    }
}

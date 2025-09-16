package com.example.bankcards.service;

import com.example.bankcards.exception.EncryptionException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class EncryptionServiceTest {

    private EncryptionService encryptionService;

    @BeforeEach
    void setUp() {
        encryptionService = new EncryptionService("TestKey123456789");
    }

    @Test
    void encrypt_ShouldReturnEncryptedString() {
        // Given
        String cardNumber = "1234567890123456";

        // When
        String encrypted = encryptionService.encrypt(cardNumber);

        // Then
        assertNotNull(encrypted);
        assertNotEquals(cardNumber, encrypted);
        assertFalse(encrypted.isEmpty());
    }

    @Test
    void decrypt_ShouldReturnOriginalString() {
        // Given
        String cardNumber = "1234567890123456";
        String encrypted = encryptionService.encrypt(cardNumber);

        // When
        String decrypted = encryptionService.decrypt(encrypted);

        // Then
        assertEquals(cardNumber, decrypted);
    }

    @Test
    void encryptDecrypt_ShouldBeReversible() {
        // Given
        String cardNumber = "4111111111111111";

        // When
        String encrypted = encryptionService.encrypt(cardNumber);
        String decrypted = encryptionService.decrypt(encrypted);

        // Then
        assertEquals(cardNumber, decrypted);
    }

    @Test
    void decrypt_WithInvalidData_ShouldThrowEncryptionException() {
        // Given
        String invalidEncryptedData = "invalid-base64-data";

        // When & Then
        assertThrows(EncryptionException.class, () -> 
            encryptionService.decrypt(invalidEncryptedData));
    }

    @Test
    void maskCardNumber_ShouldReturnMaskedNumber() {
        // Given
        String cardNumber = "1234567890123456";

        // When
        String masked = encryptionService.maskCardNumber(cardNumber);

        // Then
        assertEquals("**** **** **** 3456", masked);
    }

    @Test
    void maskCardNumber_WithShortNumber_ShouldReturnStars() {
        // Given
        String shortNumber = "123";

        // When
        String masked = encryptionService.maskCardNumber(shortNumber);

        // Then
        assertEquals("****", masked);
    }

    @Test
    void maskCardNumber_WithNull_ShouldReturnStars() {
        // When
        String masked = encryptionService.maskCardNumber(null);

        // Then
        assertEquals("****", masked);
    }

    @Test
    void encrypt_DifferentInputs_ShouldProduceDifferentOutputs() {
        // Given
        String cardNumber1 = "1234567890123456";
        String cardNumber2 = "1234567890123457";

        // When
        String encrypted1 = encryptionService.encrypt(cardNumber1);
        String encrypted2 = encryptionService.encrypt(cardNumber2);

        // Then
        assertNotEquals(encrypted1, encrypted2);
    }

    @Test
    void encrypt_SameInput_ShouldProduceDifferentOutputs() {
        // Given
        String cardNumber = "1234567890123456";

        // When
        String encrypted1 = encryptionService.encrypt(cardNumber);
        String encrypted2 = encryptionService.encrypt(cardNumber);

        // Then
        // С GCM режимом одинаковые входы дают разные результаты из-за случайного IV
        assertNotEquals(encrypted1, encrypted2);
        
        // Но оба должны дешифроваться в исходное значение
        assertEquals(cardNumber, encryptionService.decrypt(encrypted1));
        assertEquals(cardNumber, encryptionService.decrypt(encrypted2));
    }
}

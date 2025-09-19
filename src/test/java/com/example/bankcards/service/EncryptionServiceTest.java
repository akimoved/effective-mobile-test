package com.example.bankcards.service;

import com.example.bankcards.exception.EncryptionException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class EncryptionServiceTest {

    private EncryptionService encryptionService;

    @BeforeEach
    void setUp() {
        encryptionService = new EncryptionService("TestKey123456789");
    }

    @Test
    @DisplayName("Шифрование - возврат зашифрованной строки")
    void encrypt_ShouldReturnEncryptedString() {

        String cardNumber = "1234567890123456";


        String encrypted = encryptionService.encrypt(cardNumber);


        assertNotNull(encrypted);
        assertNotEquals(cardNumber, encrypted);
        assertFalse(encrypted.isEmpty());
    }

    @Test
    @DisplayName("Дешифрование - возврат исходной строки")
    void decrypt_ShouldReturnOriginalString() {

        String cardNumber = "1234567890123456";
        String encrypted = encryptionService.encrypt(cardNumber);


        String decrypted = encryptionService.decrypt(encrypted);


        assertEquals(cardNumber, decrypted);
    }

    @Test
    @DisplayName("Шифрование-дешифрование - обратимость")
    void encryptDecrypt_ShouldBeReversible() {

        String cardNumber = "4111111111111111";


        String encrypted = encryptionService.encrypt(cardNumber);
        String decrypted = encryptionService.decrypt(encrypted);


        assertEquals(cardNumber, decrypted);
    }

    @Test
    @DisplayName("Дешифрование - невалидные данные, исключение")
    void decrypt_WithInvalidData_ShouldThrowEncryptionException() {
        String invalidEncryptedData = "invalid-base64-data";

         assertThrows(EncryptionException.class, () -> encryptionService.decrypt(invalidEncryptedData));
    }

    @Test
    @DisplayName("Маскирование номера карты - стандартный номер")
    void maskCardNumber_ShouldReturnMaskedNumber() {
        String cardNumber = "1234567890123456";


        String masked = encryptionService.maskCardNumber(cardNumber);


        assertEquals("**** **** **** 3456", masked);
    }

    @Test
    @DisplayName("Маскирование - короткий номер")
    void maskCardNumber_WithShortNumber_ShouldReturnStars() {
        String shortNumber = "123";


        String masked = encryptionService.maskCardNumber(shortNumber);


        assertEquals("****", masked);
    }

    @Test
    @DisplayName("Маскирование - null вход")
    void maskCardNumber_WithNull_ShouldReturnStars() {
        String masked = encryptionService.maskCardNumber(null);


        assertEquals("****", masked);
    }

    @Test
    @DisplayName("Шифрование - разные входы, разные выходы")
    void encrypt_DifferentInputs_ShouldProduceDifferentOutputs() {
        String cardNumber1 = "1234567890123456";
        String cardNumber2 = "1234567890123457";


        String encrypted1 = encryptionService.encrypt(cardNumber1);
        String encrypted2 = encryptionService.encrypt(cardNumber2);


        assertNotEquals(encrypted1, encrypted2);
    }

    @Test
    @DisplayName("Шифрование - одинаковые входы, разные выходы (GCM)")
    void encrypt_SameInput_ShouldProduceDifferentOutputs() {
        String cardNumber = "1234567890123456";

        String encrypted1 = encryptionService.encrypt(cardNumber);
        String encrypted2 = encryptionService.encrypt(cardNumber);

        // С GCM режимом одинаковые входы дают разные результаты из-за случайного IV
        assertNotEquals(encrypted1, encrypted2);

        // Но оба должны дешифроваться в исходное значение
        assertEquals(cardNumber, encryptionService.decrypt(encrypted1));
        assertEquals(cardNumber, encryptionService.decrypt(encrypted2));
    }
}

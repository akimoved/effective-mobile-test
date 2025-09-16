package com.example.bankcards.service;

import com.example.bankcards.exception.EncryptionException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.security.SecureRandom;
import java.util.Base64;

/**
 * Сервис для шифрования/дешифрования номеров карт
 */
@Service
public class EncryptionService {

    private static final String ALGORITHM = "AES";
    private static final String TRANSFORMATION = "AES/GCM/NoPadding";
    private static final int GCM_IV_LENGTH = 12;
    private static final int GCM_TAG_LENGTH = 16;
    private final SecretKeySpec secretKey;
    private final SecureRandom secureRandom;

    public EncryptionService(@Value("${app.encryption.key:MySecretKey12345}") String key) {
        String normalizedKey = String.format("%-16s", key).substring(0, 16);
        this.secretKey = new SecretKeySpec(normalizedKey.getBytes(), ALGORITHM);
        this.secureRandom = new SecureRandom();
    }

    public String encrypt(String cardNumber) {
        try {
            byte[] iv = new byte[GCM_IV_LENGTH];
            secureRandom.nextBytes(iv);
            
            Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            GCMParameterSpec gcmParameterSpec = new GCMParameterSpec(GCM_TAG_LENGTH * 8, iv);
            cipher.init(Cipher.ENCRYPT_MODE, secretKey, gcmParameterSpec);
            
            byte[] encrypted = cipher.doFinal(cardNumber.getBytes());
            
            // Объединяем IV и зашифрованные данные
            byte[] encryptedWithIv = new byte[iv.length + encrypted.length];
            System.arraycopy(iv, 0, encryptedWithIv, 0, iv.length);
            System.arraycopy(encrypted, 0, encryptedWithIv, iv.length, encrypted.length);
            
            return Base64.getEncoder().encodeToString(encryptedWithIv);
        } catch (Exception e) {
            throw new EncryptionException("Ошибка шифрования номера карты", e);
        }
    }

    public String decrypt(String encryptedCardNumber) {
        try {
            byte[] decodedData = Base64.getDecoder().decode(encryptedCardNumber);
            
            // Извлекаем IV и зашифрованные данные
            byte[] iv = new byte[GCM_IV_LENGTH];
            byte[] encrypted = new byte[decodedData.length - GCM_IV_LENGTH];
            System.arraycopy(decodedData, 0, iv, 0, iv.length);
            System.arraycopy(decodedData, iv.length, encrypted, 0, encrypted.length);
            
            Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            GCMParameterSpec gcmParameterSpec = new GCMParameterSpec(GCM_TAG_LENGTH * 8, iv);
            cipher.init(Cipher.DECRYPT_MODE, secretKey, gcmParameterSpec);
            
            byte[] decrypted = cipher.doFinal(encrypted);
            return new String(decrypted);
        } catch (Exception e) {
            throw new EncryptionException("Ошибка дешифрования номера карты", e);
        }
    }

    public String maskCardNumber(String cardNumber) {
        if (cardNumber == null || cardNumber.length() < 4) {
            return "****";
        }
        String lastFour = cardNumber.substring(cardNumber.length() - 4);
        return "**** **** **** " + lastFour;
    }
}

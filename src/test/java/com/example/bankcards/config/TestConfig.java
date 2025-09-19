package com.example.bankcards.config;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * Конфигурация для тестов
 */
@TestConfiguration
public class TestConfig {

    /**
     * Более быстрый энкодер для тестов
     */
    @Bean
    @Primary
    public PasswordEncoder testPasswordEncoder() {
        // Используем меньшую силу для ускорения тестов
        return new BCryptPasswordEncoder(4);
    }
}

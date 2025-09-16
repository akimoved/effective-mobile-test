package com.example.bankcards;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

/**
 * Главный класс приложения для управления банковскими картами
 */
@SpringBootApplication
@EnableJpaAuditing
public class BankCardsApplication {

    public static void main(String[] args) {
        SpringApplication.run(BankCardsApplication.class, args);
    }
}

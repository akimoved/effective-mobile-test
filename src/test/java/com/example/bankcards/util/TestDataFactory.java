package com.example.bankcards.util;

import com.example.bankcards.dto.request.LoginRequest;
import com.example.bankcards.dto.request.RegisterRequest;
import com.example.bankcards.entity.User;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * Фабрика для создания тестовых данных
 */
public class TestDataFactory {

    public static RegisterRequest createValidRegisterRequest() {
        return new RegisterRequest(
                "testuser",
                "test@example.com",
                "password123",
                "Test",
                "User"
        );
    }

    public static RegisterRequest createRegisterRequest(String username, String email) {
        return new RegisterRequest(
                username,
                email,
                "password123",
                "Test",
                "User"
        );
    }

    public static LoginRequest createValidLoginRequest() {
        return new LoginRequest("testuser", "password123");
    }

    public static LoginRequest createLoginRequest(String login, String password) {
        return new LoginRequest(login, password);
    }

    public static User createUser(String username, String email, PasswordEncoder passwordEncoder) {
        return User.builder()
                .username(username)
                .email(email)
                .password(passwordEncoder.encode("password123"))
                .firstName("Test")
                .lastName("User")
                .build();
    }

    public static RegisterRequest createInvalidRegisterRequest() {
        return new RegisterRequest(
                "", // пустой username
                "invalid-email", // невалидный email
                "123", // слишком короткий пароль
                "",
                ""
        );
    }
}
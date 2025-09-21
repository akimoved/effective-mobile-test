package com.example.bankcards.integration;

import com.example.bankcards.dto.request.LoginRequest;
import com.example.bankcards.dto.request.RegisterRequest;
import com.example.bankcards.dto.response.AuthResponse;
import com.example.bankcards.repository.UserRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class AuthIntegrationTest extends BaseIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @BeforeEach
    void setUp() {
        userRepository.deleteAll();
    }

    @Test
    @DisplayName("Полный цикл: регистрация -> логин -> доступ к защищенному ресурсу")
    void fullAuthenticationFlow_ShouldWork() throws Exception {
        // 1. Регистрация
        RegisterRequest registerRequest = new RegisterRequest(
                "integrationuser",
                "integration@example.com",
                "password123",
                "Integration",
                "Test"
        );

        MvcResult registerResult = mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(registerRequest)))
                .andExpect(status().isOk())
                .andReturn();

        AuthResponse registerResponse = objectMapper.readValue(
                registerResult.getResponse().getContentAsString(),
                AuthResponse.class
        );
        assertNotNull(registerResponse.token());

        // 2. Логин с теми же данными
        LoginRequest loginRequest = new LoginRequest("integrationuser", "password123");

        MvcResult loginResult = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk())
                .andReturn();

        AuthResponse loginResponse = objectMapper.readValue(
                loginResult.getResponse().getContentAsString(),
                AuthResponse.class
        );
        assertNotNull(loginResponse.token());

        // 3. Проверка валидности токенов
        mockMvc.perform(post("/api/auth/validate")
                        .header("Authorization", "Bearer " + registerResponse.token()))
                .andExpect(status().isOk())
                .andExpect(content().string("true"));

        mockMvc.perform(post("/api/auth/validate")
                        .header("Authorization", "Bearer " + loginResponse.token()))
                .andExpect(status().isOk())
                .andExpect(content().string("true"));
    }

    @Test
    @DisplayName("Доступ к публичным эндпоинтам без аутентификации")
    void publicEndpoints_ShouldBeAccessible() throws Exception {
        // Эндпоинты аутентификации должны быть доступны без токена
        RegisterRequest registerRequest = new RegisterRequest(
                "publictest",
                "public@example.com",
                "password123",
                "Public",
                "Test"
        );

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(registerRequest)))
                .andExpect(status().isOk());

        LoginRequest loginRequest = new LoginRequest("publictest", "password123");

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("Токен должен работать для множественных запросов")
    void token_ShouldWorkForMultipleRequests() throws Exception {
        // Регистрируем пользователя и получаем токен
        RegisterRequest registerRequest = new RegisterRequest(
                "multiuser",
                "multi@example.com",
                "password123",
                "Multi",
                "User"
        );

        MvcResult result = mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(registerRequest)))
                .andExpect(status().isOk())
                .andReturn();

        AuthResponse response = objectMapper.readValue(
                result.getResponse().getContentAsString(),
                AuthResponse.class
        );

        String token = response.token();
        assertNotNull(token);

        // Делаем несколько запросов с одним и тем же токеном
        for (int i = 0; i < 3; i++) {
            mockMvc.perform(post("/api/auth/validate")
                            .header("Authorization", "Bearer " + token))
                    .andExpect(status().isOk())
                    .andExpect(content().string("true"));
        }
    }

    @Test
    @DisplayName("Неправильный токен должен возвращать false при валидации")
    void invalidToken_ShouldReturnFalse() throws Exception {
        String invalidToken = "invalid.jwt.token";

        mockMvc.perform(post("/api/auth/validate")
                        .header("Authorization", "Bearer " + invalidToken))
                .andExpect(status().isOk())
                .andExpect(content().string("false"));
    }

    @Test
    @DisplayName("Отсутствие токена должно возвращать false")
    void missingToken_ShouldReturnFalse() throws Exception {
        mockMvc.perform(post("/api/auth/validate"))
                .andExpect(status().isOk())
                .andExpect(content().string("false"));
    }

    @Test
    @DisplayName("Регистрация с дублирующимся email должна возвращать ошибку")
    void register_WithDuplicateEmail_ShouldReturnError() throws Exception {
        // Первый пользователь
        RegisterRequest request1 = new RegisterRequest(
                "user1",
                "duplicate@example.com",
                "password123",
                "User",
                "One"
        );

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request1)))
                .andExpect(status().isOk());

        // Второй пользователь с тем же email
        RegisterRequest request2 = new RegisterRequest(
                "user2",
                "duplicate@example.com",
                "password123",
                "User",
                "Two"
        );

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request2)))
                .andExpect(status().isConflict());
    }

    @Test
    @DisplayName("Неверные учетные данные при логине должны возвращать 401")
    void login_WithInvalidCredentials_ShouldReturn401() throws Exception {
        // Сначала регистрируем пользователя
        RegisterRequest registerRequest = new RegisterRequest(
                "testuser",
                "test@example.com",
                "correctpassword",
                "Test",
                "User"
        );

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(registerRequest)))
                .andExpect(status().isOk());

        // Пытаемся войти с неправильным паролем
        LoginRequest loginRequest = new LoginRequest("testuser", "wrongpassword");

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Логин по email должен работать")
    void login_WithEmail_ShouldWork() throws Exception {
        // Регистрируем пользователя
        RegisterRequest registerRequest = new RegisterRequest(
                "emailuser",
                "email@example.com",
                "password123",
                "Email",
                "User"
        );

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(registerRequest)))
                .andExpect(status().isOk());

        // Логинимся по email
        LoginRequest loginRequest = new LoginRequest("email@example.com", "password123");

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").exists());
    }
}
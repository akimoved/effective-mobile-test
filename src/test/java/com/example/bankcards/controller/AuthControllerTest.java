package com.example.bankcards.controller;

import com.example.bankcards.dto.request.LoginRequest;
import com.example.bankcards.dto.request.RegisterRequest;
import com.example.bankcards.dto.response.AuthResponse;
import com.example.bankcards.entity.User;
import com.example.bankcards.entity.enums.RoleName;
import com.example.bankcards.integration.BaseIntegrationTest;
import com.example.bankcards.repository.UserRepository;
import com.example.bankcards.service.JwtService;
import com.example.bankcards.service.UserService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
@Transactional
class AuthControllerTest extends BaseIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private UserService userService;

    @Autowired
    private JwtService jwtService;

    @BeforeEach
    void setUp() {
        userRepository.deleteAll();
    }

    @Test
    @DisplayName("Успешная аутентификация по username")
    void register_ShouldReturnAuthResponse_WhenValidRequest() throws Exception {
        RegisterRequest request = new RegisterRequest(
                "testuser",
                "test@example.com",
                "password123",
                "Test",
                "User"
        );

        MvcResult result = mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.token").exists())
                .andReturn();

        // Проверяем, что пользователь создался в базе
        assertThat(userRepository.findByUsername("testuser")).isPresent();

        // Проверяем валидность возвращенного токена
        String responseJson = result.getResponse().getContentAsString();
        AuthResponse response = objectMapper.readValue(responseJson, AuthResponse.class);
        assertThat(jwtService.extractUsername(response.token())).isEqualTo("testuser");
        assertThat(jwtService.isTokenValid(response.token(),
                org.springframework.security.core.userdetails.User.builder()
                        .username("testuser")
                        .password("dummy")
                        .authorities("ROLE_USER")
                        .build())).isTrue();
    }

    @Test
    @DisplayName("Регистрация с существующим username - должна вернуть ошибку")
    void register_ShouldReturnError_WhenUsernameAlreadyExists() throws Exception {
        // Given
        createTestUser("existinguser", "existing@example.com");

        RegisterRequest request = new RegisterRequest(
                "existinguser",
                "new@example.com",
                "password123",
                "New",
                "User"
        );

        // When & Then
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict());
    }

    @Test
    @DisplayName("Регистрация с существующим email - должна вернуть ошибку")
    void register_ShouldReturnError_WhenEmailAlreadyExists() throws Exception {
        createTestUser("user1", "existing@example.com");

        RegisterRequest request = new RegisterRequest(
                "user2",
                "existing@example.com",
                "password123",
                "User",
                "Two"
        );

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict());
    }

    @Test
    @DisplayName("Регистрация с невалидными данными")
    void register_ShouldReturnValidationError_WhenInvalidData() throws Exception {
        // Пустой username и невалидный email
        RegisterRequest request = new RegisterRequest(
                "",
                "invalid-email",
                "123", // слишком короткий пароль
                "",
                ""
        );

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Успешная аутентификация по username")
    void login_ShouldReturnAuthResponse_WhenValidCredentialsWithUsername() throws Exception {
        createTestUser("testuser", "test@example.com");

        LoginRequest request = new LoginRequest("testuser", "password123");

        MvcResult result = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.token").exists())
                .andReturn();

        // Проверяем валидность токена
        String responseJson = result.getResponse().getContentAsString();
        AuthResponse response = objectMapper.readValue(responseJson, AuthResponse.class);
        assertThat(jwtService.extractUsername(response.token())).isEqualTo("testuser");
    }

    @Test
    @DisplayName("Успешная аутентификация по email")
    void login_ShouldReturnAuthResponse_WhenValidCredentialsWithEmail() throws Exception {
        createTestUser("testuser", "test@example.com");

        LoginRequest request = new LoginRequest("test@example.com", "password123");

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.token").exists());
    }

    @Test
    @DisplayName("Аутентификация с неверными credentials")
    void login_ShouldReturnUnauthorized_WhenInvalidCredentials() throws Exception {
        createTestUser("testuser", "test@example.com");

        LoginRequest request = new LoginRequest("testuser", "wrongpassword");

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Аутентификация несуществующего пользователя")
    void login_ShouldReturnUnauthorized_WhenUserNotExists() throws Exception {
        LoginRequest request = new LoginRequest("nonexistent", "password123");

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Валидация токена - валидный токен")
    void validateToken_ShouldReturnTrue_WhenValidToken() throws Exception {
        User user = createTestUser("testuser", "test@example.com");
        String token = jwtService.generateToken(
                org.springframework.security.core.userdetails.User.builder()
                        .username(user.getUsername())
                        .password(user.getPassword())
                        .authorities("USER")
                        .build()
        );

        mockMvc.perform(post("/api/auth/validate")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(content().string("true"));
    }

    @Test
    @DisplayName("Валидация токена - невалидный токен")
    void validateToken_ShouldReturnFalse_WhenInvalidToken() throws Exception {
        String invalidToken = "invalid.jwt.token";

        mockMvc.perform(post("/api/auth/validate")
                        .header("Authorization", "Bearer " + invalidToken))
                .andExpect(status().isOk())
                .andExpect(content().string("false"));
    }

    @Test
    @DisplayName("Валидация токена - отсутствует заголовок")
    void validateToken_ShouldReturnFalse_WhenNoAuthHeader() throws Exception {
        mockMvc.perform(post("/api/auth/validate"))
                .andExpect(status().isOk())
                .andExpect(content().string("false"));
    }

    @Test
    @DisplayName("Валидация токена - неправильный формат заголовка")
    void validateToken_ShouldReturnFalse_WhenInvalidAuthHeader() throws Exception {
        mockMvc.perform(post("/api/auth/validate")
                        .header("Authorization", "Invalid token"))
                .andExpect(status().isOk())
                .andExpect(content().string("false"));
    }

    private User createTestUser(String username, String email) {
        return userService.createUser(
                username,
                email,
                "password123",  // UserService сам закодирует пароль
                "Test",
                "User",
                RoleName.ROLE_USER
        );
    }
}

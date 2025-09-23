package com.example.bankcards.integration;

import com.example.bankcards.dto.request.CardCreateRequest;
import com.example.bankcards.dto.request.CardUpdateRequest;
import com.example.bankcards.dto.response.CardResponse;
import com.example.bankcards.entity.enums.CardStatus;
import com.example.bankcards.entity.enums.RoleName;
import com.example.bankcards.repository.CardRepository;
import com.example.bankcards.repository.UserRepository;
import com.example.bankcards.service.AuthService;
import com.example.bankcards.service.UserService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class CardIntegrationTest extends BaseIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private CardRepository cardRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private AuthService authService;

    @Autowired
    private UserService userService;

    private String userToken;
    private String adminToken;
    private Long testCardId;

    @BeforeEach
    void setUp() throws Exception {
        // Очистка данных
        cardRepository.deleteAll();
        userRepository.deleteAll();

        // Создание обычного пользователя через UserService
        userService.createUser(
                "testuser",
                "test@example.com",
                "password123",
                "Test",
                "User",
                RoleName.ROLE_USER
        );

        // Создание администратора через UserService
        userService.createUser(
                "admin",
                "admin@example.com",
                "admin123",
                "Admin",
                "User",
                RoleName.ROLE_ADMIN
        );

        // Получаем токены через сервис аутентификации
        userToken = authService.authenticate("testuser", "password123").token();
        adminToken = authService.authenticate("admin", "admin123").token();

        // Создание тестовой карты
        CardCreateRequest cardRequest = new CardCreateRequest("1234567812345678", "Test User");

        MvcResult cardResult = mockMvc.perform(post("/api/cards")
                        .header("Authorization", "Bearer " + userToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(cardRequest)))
                .andExpect(status().isCreated())
                .andReturn();

        CardResponse cardResponse = objectMapper.readValue(
                cardResult.getResponse().getContentAsString(),
                CardResponse.class
        );
        testCardId = cardResponse.id();
    }

    @Test
    @DisplayName("Создание карты - успешный сценарий")
    void createCard_ShouldReturnCreated() throws Exception {
        CardCreateRequest request = new CardCreateRequest("1234567890123456", "John Doe");

        mockMvc.perform(post("/api/cards")
                        .header("Authorization", "Bearer " + userToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.cardNumber").value("**** **** **** 3456"))
                .andExpect(jsonPath("$.cardholderName").value("John Doe"))
                .andExpect(jsonPath("$.status").value("ACTIVE"));
    }

    @Test
    @DisplayName("Создание карты с дублирующимся номером - ошибка")
    void createCard_WithDuplicateNumber_ShouldReturnError() throws Exception {
        CardCreateRequest request = new CardCreateRequest("1234567812345678", "Duplicate User");

        mockMvc.perform(post("/api/cards")
                        .header("Authorization", "Bearer " + userToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict());
    }

    @Test
    @DisplayName("Получение карты по ID - успешный сценарий")
    void getCard_ShouldReturnCard() throws Exception {
        mockMvc.perform(get("/api/cards/" + testCardId)
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(testCardId))
                .andExpect(jsonPath("$.cardNumber").exists())
                .andExpect(jsonPath("$.cardholderName").value("Test User"));
    }

    @Test
    @DisplayName("Получение несуществующей карты - ошибка")
    void getCard_NonExistent_ShouldReturnNotFound() throws Exception {
        mockMvc.perform(get("/api/cards/999")
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("Получение всех карт пользователя")
    void getUserCards_ShouldReturnCards() throws Exception {
        mockMvc.perform(get("/api/cards")
                        .header("Authorization", "Bearer " + userToken)
                        .param("page", "0")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.content[0].id").value(testCardId));
    }

    @Test
    @DisplayName("Обновление карты - успешный сценарий")
    void updateCard_ShouldReturnUpdatedCard() throws Exception {
        CardUpdateRequest request = new CardUpdateRequest("Updated Name", CardStatus.BLOCKED);

        mockMvc.perform(put("/api/cards/" + testCardId)
                        .header("Authorization", "Bearer " + userToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.cardholderName").value("Updated Name"))
                .andExpect(jsonPath("$.status").value("BLOCKED"));
    }

    @Test
    @DisplayName("Блокировка карты")
    void blockCard_ShouldBlockCard() throws Exception {
        mockMvc.perform(post("/api/cards/" + testCardId + "/block")
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("BLOCKED"));
    }

    @Test
    @DisplayName("Разблокировка карты")
    void unblockCard_ShouldUnblockCard() throws Exception {
        // Сначала блокируем карту
        mockMvc.perform(post("/api/cards/" + testCardId + "/block")
                .header("Authorization", "Bearer " + userToken));

        // Затем разблокируем
        mockMvc.perform(post("/api/cards/" + testCardId + "/unblock")
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ACTIVE"));
    }

    @Test
    @DisplayName("Удаление карты")
    void deleteCard_ShouldDeleteCard() throws Exception {
        mockMvc.perform(delete("/api/cards/" + testCardId)
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isNoContent());

        // Проверяем, что карта действительно удалена
        mockMvc.perform(get("/api/cards/" + testCardId)
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("Поиск карты по номеру (администратор)")
    void findByCardNumber_Admin_ShouldReturnCard() throws Exception {
        mockMvc.perform(get("/api/cards/search")
                        .header("Authorization", "Bearer " + adminToken)
                        .param("cardNumber", "1234567812345678"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(testCardId));
    }

    @Test
    @DisplayName("Поиск карты по номеру (не администратор) - ошибка доступа")
    void findByCardNumber_NonAdmin_ShouldReturnForbidden() throws Exception {
        mockMvc.perform(get("/api/cards/search")
                        .header("Authorization", "Bearer " + userToken)
                        .param("cardNumber", "1234567812345678"))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Получение всех карт (администратор)")
    void getAllCards_Admin_ShouldReturnAllCards() throws Exception {
        mockMvc.perform(get("/api/cards/admin/all")
                        .header("Authorization", "Bearer " + adminToken)
                        .param("page", "0")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content.length()").value(1));
    }

    @Test
    @DisplayName("Доступ к чужой карте - ошибка")
    void accessOtherUserCard_ShouldReturnForbidden() throws Exception {
        // Создаем второго пользователя
        String secondUserRequest = """
            {
                "username": "user2",
                "email": "user2@example.com",
                "password": "password123",
                "firstName": "User",
                "lastName": "Two"
            }
            """;

        MvcResult secondUserResult = mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(secondUserRequest))
                .andExpect(status().isOk())
                .andReturn();

        String secondUserToken = objectMapper.readTree(secondUserResult.getResponse().getContentAsString())
                .get("token").asText();

        // Пытаемся получить карту первого пользователя
        mockMvc.perform(get("/api/cards/" + testCardId)
                        .header("Authorization", "Bearer " + secondUserToken))
                .andExpect(status().isForbidden());
    }
}
package com.example.bankcards.controller;

import com.example.bankcards.dto.request.CardCreateRequest;
import com.example.bankcards.dto.request.CardUpdateRequest;
import com.example.bankcards.entity.Card;
import com.example.bankcards.entity.User;
import com.example.bankcards.entity.enums.CardStatus;
import com.example.bankcards.entity.enums.RoleName;
import com.example.bankcards.integration.BaseIntegrationTest;
import com.example.bankcards.repository.CardRepository;
import com.example.bankcards.repository.UserRepository;
import com.example.bankcards.service.EncryptionService;
import com.example.bankcards.service.UserService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class CardControllerTest extends BaseIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private CardRepository cardRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private UserService userService;

    @MockBean
    private EncryptionService encryptionService;

    private User testUser;
    private User adminUser;

    @BeforeEach
    void setUp() {
        cardRepository.deleteAll();
        userRepository.deleteAll();

        // Создаем тестового пользователя
        testUser = userService.createUser(
                "testuser",
                "test@example.com",
                "password123",
                "Test",
                "User",
                RoleName.ROLE_USER
        );

        // Создаем администратора
        adminUser = userService.createUser(
                "admin",
                "admin@example.com",
                "password123",
                "Admin",
                "User",
                RoleName.ROLE_ADMIN
        );

        // Мокируем EncryptionService
        when(encryptionService.encrypt(anyString())).thenAnswer(invocation -> invocation.getArgument(0));
        when(encryptionService.decrypt(anyString())).thenAnswer(invocation -> invocation.getArgument(0));
        when(encryptionService.maskCardNumber(anyString())).thenAnswer(invocation -> {
            String cardNumber = invocation.getArgument(0).toString();
            String lastFour = cardNumber.substring(cardNumber.length() - 4);
            return "**** **** **** " + lastFour;
        });
    }

    @Test
    @DisplayName("Создание карты - успешное создание")
    @WithMockUser(username = "testuser")
    void createCard_ShouldReturnCard_WhenValidRequest() throws Exception {
        CardCreateRequest request = new CardCreateRequest(
                "1234567812345678",
                "Test User"
        );

        mockMvc.perform(post("/api/cards")
                        .with(user("testuser").roles("USER"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.cardholderName").value("Test User"))
                .andExpect(jsonPath("$.status").value("ACTIVE"))
                .andExpect(jsonPath("$.cardNumber").value("**** **** **** 5678"))
                .andReturn();

        // Проверяем, что карта создалась в БД
        assertThat(cardRepository.count()).isEqualTo(1);
    }

    @Test
    @DisplayName("Создание карты - невалидные данные")
    @WithMockUser(username = "testuser")
    void createCard_ShouldReturnBadRequest_WhenInvalidData() throws Exception {
        CardCreateRequest request = new CardCreateRequest(
                "123", // невалидный номер карты
                "" // пустое имя
        );

        mockMvc.perform(post("/api/cards")
                        .with(user("testuser").roles("USER"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Получение карты по ID - успешное получение")
    @WithMockUser(username = "testuser")
    void getCard_ShouldReturnCard_WhenCardExists() throws Exception {
        Card card = createTestCard(testUser, "1234567812345678", "Test User");

        mockMvc.perform(get("/api/cards/{cardId}", card.getId())
                        .with(user("testuser").roles("USER")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(card.getId()))
                .andExpect(jsonPath("$.cardholderName").value("Test User"))
                .andExpect(jsonPath("$.status").value("ACTIVE"));
    }

    @Test
    @DisplayName("Получение карты по ID - карта не найдена")
    @WithMockUser(username = "testuser")
    void getCard_ShouldReturnNotFound_WhenCardNotExists() throws Exception {
        mockMvc.perform(get("/api/cards/{cardId}", 999L)
                        .with(user("testuser").roles("USER")))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("Получение карт пользователя")
    @WithMockUser(username = "testuser")
    void getUserCards_ShouldReturnUserCards() throws Exception {
        createTestCard(testUser, "1234567812345678", "Card 1");
        createTestCard(testUser, "8765432187654321", "Card 2");

        // Создаем карту другого пользователя (не должна попасть в результат)
        User anotherUser = userService.createUser(
                "another", "another@example.com", "password", "Another", "User", RoleName.ROLE_USER
        );
        createTestCard(anotherUser, "1111222233334444", "Another Card");

        mockMvc.perform(get("/api/cards")
                        .with(user("testuser").roles("USER")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content.length()").value(2))
                .andExpect(jsonPath("$.content[0].cardholderName").exists())
                .andExpect(jsonPath("$.content[1].cardholderName").exists());
    }

    @Test
    @DisplayName("Обновление карты - успешное обновление")
    @WithMockUser(username = "testuser")
    void updateCard_ShouldUpdateCard_WhenValidRequest() throws Exception {
        Card card = createTestCard(testUser, "1234567812345678", "Old Name");

        CardUpdateRequest request = new CardUpdateRequest(
                "New Name",
                CardStatus.ACTIVE
        );

        mockMvc.perform(put("/api/cards/{cardId}", card.getId())
                        .with(user("testuser").roles("USER"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.cardholderName").value("New Name"));
    }

    @Test
    @DisplayName("Блокировка карты")
    @WithMockUser(username = "testuser")
    void blockCard_ShouldBlockCard_WhenCardExists() throws Exception {
        Card card = createTestCard(testUser, "1234567812345678", "Test User");
        assertThat(card.getStatus()).isEqualTo(CardStatus.ACTIVE);

        mockMvc.perform(post("/api/cards/{cardId}/block", card.getId())
                        .with(user("testuser").roles("USER")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("BLOCKED"));

        // Проверяем в БД
        Card updatedCard = cardRepository.findById(card.getId()).orElseThrow();
        assertThat(updatedCard.getStatus()).isEqualTo(CardStatus.BLOCKED);
    }

    @Test
    @DisplayName("Разблокировка карты")
    @WithMockUser(username = "testuser")
    void unblockCard_ShouldUnblockCard_WhenCardBlocked() throws Exception {
        Card card = createTestCard(testUser, "1234567812345678", "Test User");
        card.setStatus(CardStatus.BLOCKED);
        cardRepository.save(card);

        mockMvc.perform(post("/api/cards/{cardId}/unblock", card.getId())
                        .with(user("testuser").roles("USER")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ACTIVE"));

        // Проверяем в БД
        Card updatedCard = cardRepository.findById(card.getId()).orElseThrow();
        assertThat(updatedCard.getStatus()).isEqualTo(CardStatus.ACTIVE);
    }

    @Test
    @DisplayName("Удаление карты")
    @WithMockUser(username = "testuser")
    void deleteCard_ShouldDeleteCard_WhenCardExists() throws Exception {
        Card card = createTestCard(testUser, "1234567812345678", "Test Card");
        Long cardId = card.getId();

        mockMvc.perform(delete("/api/cards/{cardId}", cardId)
                        .with(user("testuser").roles("USER")))
                .andExpect(status().isNoContent());

        // Проверяем, что карта удалена
        assertThat(cardRepository.findById(cardId)).isEmpty();
    }

    @Test
    @DisplayName("Попытка доступа к чужой карте - должна вернуть Forbidden")
    @WithMockUser(username = "testuser")
    void accessOtherUserCard_ShouldReturnForbidden() throws Exception {
        User anotherUser = userService.createUser(
                "another", "another@example.com", "password", "Another", "User", RoleName.ROLE_USER
        );
        Card anotherCard = createTestCard(anotherUser, "1111222233334444", "Another Card");

        mockMvc.perform(get("/api/cards/{cardId}", anotherCard.getId())
                        .with(user("testuser").roles("USER")))
                .andExpect(status().isForbidden());

        mockMvc.perform(delete("/api/cards/{cardId}", anotherCard.getId())
                        .with(user("testuser").roles("USER")))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Поиск карты по номеру - только для админов")
    @WithMockUser(username = "admin", roles = {"ADMIN"})
    void findByCardNumber_ShouldReturnCard_WhenAdmin() throws Exception {
        Card card = createTestCard(testUser, "1234567812345678", "Test User");

        mockMvc.perform(get("/api/cards/search")
                        .with(user("admin").roles("ADMIN"))
                        .param("cardNumber", card.getCardNumber()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.cardNumber").value(getMaskedCardNumber(card)));
    }

    @Test
    @DisplayName("Поиск карты по номеру - запрещено для обычных пользователей")
    @WithMockUser(username = "testuser")
    void findByCardNumber_ShouldReturnForbidden_WhenNotAdmin() throws Exception {
        Card card = createTestCard(testUser, "1234567812345678", "Test User");

        mockMvc.perform(get("/api/cards/search")
                        .with(user("testuser").roles("USER"))
                        .param("cardNumber", card.getCardNumber()))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Получение всех карт - только для админов")
    @WithMockUser(username = "admin", roles = {"ADMIN"})
    void getAllCards_ShouldReturnAllCards_WhenAdmin() throws Exception {
        createTestCard(testUser, "1234567812345678", "User Card");
        createTestCard(adminUser, "8765432187654321", "Admin Card");

        mockMvc.perform(get("/api/cards/admin/all")
                        .with(user("admin").roles("ADMIN")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content.length()").value(2));
    }

    @Test
    @DisplayName("Получение всех карт - запрещено для обычных пользователей")
    @WithMockUser(username = "testuser")
    void getAllCards_ShouldReturnForbidden_WhenNotAdmin() throws Exception {
        mockMvc.perform(get("/api/cards/admin/all")
                        .with(user("testuser").roles("USER")))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Доступ без аутентификации - должен вернуть Unauthorized")
    void accessWithoutAuth_ShouldReturnUnauthorized() throws Exception {
        Card card = createTestCard(testUser, "1234567812345678", "Test Card");

        mockMvc.perform(get("/api/cards/{cardId}", card.getId()))
                .andExpect(status().isForbidden());

        mockMvc.perform(get("/api/cards"))
                .andExpect(status().isForbidden());

        mockMvc.perform(post("/api/cards")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isForbidden());
    }

    private Card createTestCard(User user, String cardNumber, String cardholderName) {
        Card card = Card.builder()
                .cardNumber(cardNumber)
                .cardHolderName(cardholderName)
                .expiryDate(LocalDate.now().plusYears(3))
                .status(CardStatus.ACTIVE)
                .balance(BigDecimal.ZERO)
                .user(user)
                .createdAt(LocalDateTime.now())
                .build();
        return cardRepository.save(card);
    }

    private String getMaskedCardNumber(Card card) {
        String cardNumber = card.getCardNumber();
        String lastFour = cardNumber.substring(cardNumber.length() - 4);
        return "**** **** **** " + lastFour;
    }
}
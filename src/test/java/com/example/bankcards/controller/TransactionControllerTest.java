package com.example.bankcards.controller;

import com.example.bankcards.dto.request.TransactionCreateRequest;
import com.example.bankcards.entity.Card;
import com.example.bankcards.entity.Transaction;
import com.example.bankcards.entity.User;
import com.example.bankcards.entity.enums.CardStatus;
import com.example.bankcards.entity.enums.RoleName;
import com.example.bankcards.entity.enums.TransactionStatus;
import com.example.bankcards.integration.BaseIntegrationTest;
import com.example.bankcards.repository.CardRepository;
import com.example.bankcards.repository.TransactionRepository;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class TransactionControllerTest extends BaseIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private TransactionRepository transactionRepository;

    @Autowired
    private CardRepository cardRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private UserService userService;

    @MockBean
    private EncryptionService encryptionService;

    private User anotherUser;
    private Card testCard1;
    private Card testCard2;
    private Card anotherUserCard;

    @BeforeEach
    void setUp() {
        transactionRepository.deleteAll();
        cardRepository.deleteAll();
        userRepository.deleteAll();

        // Создаем тестового пользователя
        User testUser = userService.createUser(
                "testuser",
                "test@example.com",
                "password123",
                "Test",
                "User",
                RoleName.ROLE_USER
        );

        // Создаем администратора
        userService.createUser(
                "admin",
                "admin@example.com",
                "password123",
                "Admin",
                "User",
                RoleName.ROLE_ADMIN
        );

        // Создаем второго пользователя
        anotherUser = userService.createUser(
                "another",
                "another@example.com",
                "password123",
                "Another",
                "User",
                RoleName.ROLE_USER
        );

        // Создаем карты для тестов
        testCard1 = createTestCard(testUser, "1234567812345678", "Test Card 1", new BigDecimal("1000.00"));
        testCard2 = createTestCard(testUser, "8765432187654321", "Test Card 2", new BigDecimal("500.00"));
        anotherUserCard = createTestCard(anotherUser, "1111222233334444", "Another Card", new BigDecimal("200.00"));

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
    @DisplayName("Создание перевода - успешный перевод между своими картами")
    @WithMockUser(username = "testuser")
    void createTransfer_ShouldReturnTransaction_WhenValidTransfer() throws Exception {
        TransactionCreateRequest request = new TransactionCreateRequest(
                testCard1.getId(),
                testCard2.getId(),
                new BigDecimal("100.00"),
                "Test transfer"
        );

        mockMvc.perform(post("/api/transactions")
                        .with(user("testuser").roles("USER"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.fromCardId").value(testCard1.getId()))
                .andExpect(jsonPath("$.toCardId").value(testCard2.getId()))
                .andExpect(jsonPath("$.amount").value(100.00))
                .andExpect(jsonPath("$.description").value("Test transfer"))
                .andExpect(jsonPath("$.status").value("COMPLETED"))
                .andExpect(jsonPath("$.fromCardNumber").value("**** **** **** 5678"))
                .andExpect(jsonPath("$.toCardNumber").value("**** **** **** 4321"));

        // Проверяем, что транзакция создалась в БД
        assertThat(transactionRepository.count()).isEqualTo(1);

        // Проверяем, что балансы карт обновились
        Card updatedFromCard = cardRepository.findById(testCard1.getId()).orElseThrow();
        Card updatedToCard = cardRepository.findById(testCard2.getId()).orElseThrow();

        assertThat(updatedFromCard.getBalance()).isEqualTo(new BigDecimal("900.00"));
        assertThat(updatedToCard.getBalance()).isEqualTo(new BigDecimal("600.00"));
    }

    @Test
    @DisplayName("Создание перевода - невалидные данные")
    @WithMockUser(username = "testuser")
    void createTransfer_ShouldReturnBadRequest_WhenInvalidData() throws Exception {
        TransactionCreateRequest request = new TransactionCreateRequest(
                null, // null fromCardId
                testCard2.getId(),
                new BigDecimal("-100.00"), // отрицательная сумма
                "Invalid transfer"
        );

        mockMvc.perform(post("/api/transactions")
                        .with(user("testuser").roles("USER"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Создание перевода - недостаточно средств")
    @WithMockUser(username = "testuser")
    void createTransfer_ShouldReturnBadRequest_WhenInsufficientFunds() throws Exception {
        TransactionCreateRequest request = new TransactionCreateRequest(
                testCard2.getId(), // у этой карты только 500
                testCard1.getId(),
                new BigDecimal("1000.00"), // переводим больше чем есть
                "Insufficient funds transfer"
        );

        mockMvc.perform(post("/api/transactions")
                        .with(user("testuser").roles("USER"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Создание перевода - перевод на ту же карту")
    @WithMockUser(username = "testuser")
    void createTransfer_ShouldReturnBadRequest_WhenSameCard() throws Exception {
        TransactionCreateRequest request = new TransactionCreateRequest(
                testCard1.getId(),
                testCard1.getId(), // та же карта
                new BigDecimal("100.00"),
                "Same card transfer"
        );

        mockMvc.perform(post("/api/transactions")
                        .with(user("testuser").roles("USER"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Создание перевода - попытка перевода на чужую карту")
    @WithMockUser(username = "testuser")
    void createTransfer_ShouldReturnBadRequest_WhenTransferToAnotherUserCard() throws Exception {
        TransactionCreateRequest request = new TransactionCreateRequest(
                testCard1.getId(),
                anotherUserCard.getId(), // чужая карта
                new BigDecimal("100.00"),
                "Transfer to another user"
        );

        mockMvc.perform(post("/api/transactions")
                        .with(user("testuser").roles("USER"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Создание перевода - заблокированная карта отправителя")
    @WithMockUser(username = "testuser")
    void createTransfer_ShouldReturnBadRequest_WhenFromCardBlocked() throws Exception {
        // Блокируем карту отправителя
        testCard1.setStatus(CardStatus.BLOCKED);
        cardRepository.save(testCard1);

        TransactionCreateRequest request = new TransactionCreateRequest(
                testCard1.getId(),
                testCard2.getId(),
                new BigDecimal("100.00"),
                "Transfer from blocked card"
        );

        mockMvc.perform(post("/api/transactions")
                        .with(user("testuser").roles("USER"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Получение транзакции по ID - успешное получение")
    @WithMockUser(username = "testuser")
    void getTransaction_ShouldReturnTransaction_WhenTransactionExists() throws Exception {
        Transaction transaction = createTestTransaction(testCard1, testCard2, new BigDecimal("50.00"), "Test transaction");

        mockMvc.perform(get("/api/transactions/{transactionId}", transaction.getId())
                        .with(user("testuser").roles("USER")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(transaction.getId()))
                .andExpect(jsonPath("$.amount").value(50.00))
                .andExpect(jsonPath("$.description").value("Test transaction"))
                .andExpect(jsonPath("$.status").value("COMPLETED"));
    }

    @Test
    @DisplayName("Получение транзакции по ID - транзакция не найдена")
    @WithMockUser(username = "testuser")
    void getTransaction_ShouldReturnNotFound_WhenTransactionNotExists() throws Exception {
        mockMvc.perform(get("/api/transactions/{transactionId}", 999L)
                        .with(user("testuser").roles("USER")))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("Получение транзакций пользователя")
    @WithMockUser(username = "testuser")
    void getUserTransactions_ShouldReturnUserTransactions() throws Exception {
        createTestTransaction(testCard1, testCard2, new BigDecimal("100.00"), "Transaction 1");
        createTestTransaction(testCard2, testCard1, new BigDecimal("50.00"), "Transaction 2");

        // Создаем транзакцию другого пользователя (не должна попасть в результат)
        Card anotherCard1 = createTestCard(anotherUser, "5555666677778888", "Another Card 2", new BigDecimal("300.00"));
        createTestTransaction(anotherUserCard, anotherCard1, new BigDecimal("25.00"), "Another transaction");

        mockMvc.perform(get("/api/transactions")
                        .with(user("testuser").roles("USER")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content.length()").value(2));
    }

    @Test
    @DisplayName("Получение транзакций карты")
    @WithMockUser(username = "testuser")
    void getCardTransactions_ShouldReturnCardTransactions() throws Exception {
        createTestTransaction(testCard1, testCard2, new BigDecimal("100.00"), "From card1 to card2");
        createTestTransaction(testCard2, testCard1, new BigDecimal("50.00"), "From card2 to card1");
        createTestTransaction(testCard1, testCard2, new BigDecimal("25.00"), "Another from card1");

        mockMvc.perform(get("/api/transactions/card/{cardId}", testCard1.getId())
                        .with(user("testuser").roles("USER")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content.length()").value(3)); // все 3 транзакции касаются card1
    }

    @Test
    @DisplayName("Получение внутренних переводов")
    @WithMockUser(username = "testuser")
    void getInternalTransfers_ShouldReturnInternalTransfers() throws Exception {
        createTestTransaction(testCard1, testCard2, new BigDecimal("100.00"), "Internal transfer");

        mockMvc.perform(get("/api/transactions/internal")
                        .with(user("testuser").roles("USER")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content.length()").value(1));
    }

    @Test
    @DisplayName("Получение баланса карты")
    @WithMockUser(username = "testuser")
    void getCardBalance_ShouldReturnBalance_WhenCardExists() throws Exception {
        mockMvc.perform(get("/api/transactions/balance/{cardId}", testCard1.getId())
                        .with(user("testuser").roles("USER")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.cardId").value(testCard1.getId()))
                .andExpect(jsonPath("$.balance").value(1000.00))
                .andExpect(jsonPath("$.cardholderName").value("Test Card 1"))
                .andExpect(jsonPath("$.maskedCardNumber").value("**** **** **** 5678"));
    }

    @Test
    @DisplayName("Получение баланса карты - карта не найдена")
    @WithMockUser(username = "testuser")
    void getCardBalance_ShouldReturnNotFound_WhenCardNotExists() throws Exception {
        mockMvc.perform(get("/api/transactions/balance/{cardId}", 999L)
                        .with(user("testuser").roles("USER")))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("Получение балансов всех карт пользователя")
    @WithMockUser(username = "testuser")
    void getUserBalances_ShouldReturnAllUserBalances() throws Exception {
        mockMvc.perform(get("/api/transactions/balances")
                        .with(user("testuser").roles("USER")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content.length()").value(2))
                .andExpect(jsonPath("$.content[0].balance").exists())
                .andExpect(jsonPath("$.content[1].balance").exists());
    }

    @Test
    @DisplayName("Попытка доступа к чужой транзакции - должна вернуть Forbidden")
    @WithMockUser(username = "testuser")
    void accessOtherUserTransaction_ShouldReturnForbidden() throws Exception {
        Card anotherCard1 = createTestCard(anotherUser, "5555666677778888", "Another Card 2", new BigDecimal("300.00"));
        Transaction anotherTransaction = createTestTransaction(anotherUserCard, anotherCard1, new BigDecimal("50.00"), "Another user transaction");

        mockMvc.perform(get("/api/transactions/{transactionId}", anotherTransaction.getId())
                        .with(user("testuser").roles("USER")))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Попытка доступа к чужой карте для получения баланса - должна вернуть Forbidden")
    @WithMockUser(username = "testuser")
    void accessOtherUserCardBalance_ShouldReturnForbidden() throws Exception {
        mockMvc.perform(get("/api/transactions/balance/{cardId}", anotherUserCard.getId())
                        .with(user("testuser").roles("USER")))
                .andExpect(status().isForbidden());

        mockMvc.perform(get("/api/transactions/card/{cardId}", anotherUserCard.getId())
                        .with(user("testuser").roles("USER")))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Получение всех транзакций - только для админов")
    @WithMockUser(username = "admin", roles = {"ADMIN"})
    void getAllTransactions_ShouldReturnAllTransactions_WhenAdmin() throws Exception {
        createTestTransaction(testCard1, testCard2, new BigDecimal("100.00"), "User transaction");

        Card anotherCard1 = createTestCard(anotherUser, "5555666677778888", "Another Card 2", new BigDecimal("300.00"));
        createTestTransaction(anotherUserCard, anotherCard1, new BigDecimal("50.00"), "Another user transaction");

        mockMvc.perform(get("/api/transactions/admin/all")
                        .with(user("admin").roles("ADMIN")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content.length()").value(2));
    }

    @Test
    @DisplayName("Получение всех транзакций - запрещено для обычных пользователей")
    @WithMockUser(username = "testuser")
    void getAllTransactions_ShouldReturnForbidden_WhenNotAdmin() throws Exception {
        mockMvc.perform(get("/api/transactions/admin/all")
                        .with(user("testuser").roles("USER")))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Доступ без аутентификации - должен вернуть Forbidden")
    void accessWithoutAuth_ShouldReturnForbidden() throws Exception {
        Transaction transaction = createTestTransaction(testCard1, testCard2, new BigDecimal("100.00"), "Test");

        mockMvc.perform(get("/api/transactions/{transactionId}", transaction.getId()))
                .andExpect(status().isForbidden());

        mockMvc.perform(get("/api/transactions"))
                .andExpect(status().isForbidden());

        mockMvc.perform(post("/api/transactions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isForbidden());

        mockMvc.perform(get("/api/transactions/balance/{cardId}", testCard1.getId()))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Создание перевода с минимальной суммой")
    @WithMockUser(username = "testuser")
    void createTransfer_ShouldWork_WhenMinimalAmount() throws Exception {
        TransactionCreateRequest request = new TransactionCreateRequest(
                testCard1.getId(),
                testCard2.getId(),
                new BigDecimal("0.01"), // минимальная сумма
                "Minimal transfer"
        );

        mockMvc.perform(post("/api/transactions")
                        .with(user("testuser").roles("USER"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.amount").value(0.01));
    }

    @Test
    @DisplayName("Создание перевода с максимальной суммой")
    @WithMockUser(username = "testuser")
    void createTransfer_ShouldReturnBadRequest_WhenAmountTooLarge() throws Exception {
        TransactionCreateRequest request = new TransactionCreateRequest(
                testCard1.getId(),
                testCard2.getId(),
                new BigDecimal("1000001.00"), // превышает лимит в 1,000,000
                "Too large transfer"
        );

        mockMvc.perform(post("/api/transactions")
                        .with(user("testuser").roles("USER"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Создание перевода без описания")
    @WithMockUser(username = "testuser")
    void createTransfer_ShouldWork_WhenNoDescription() throws Exception {
        TransactionCreateRequest request = new TransactionCreateRequest(
                testCard1.getId(),
                testCard2.getId(),
                new BigDecimal("100.00"),
                null // без описания
        );

        mockMvc.perform(post("/api/transactions")
                        .with(user("testuser").roles("USER"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.description").isEmpty());
    }

    private Card createTestCard(User user, String cardNumber, String cardholderName, BigDecimal balance) {
        Card card = Card.builder()
                .cardNumber(cardNumber)
                .cardHolderName(cardholderName)
                .expiryDate(LocalDate.now().plusYears(3))
                .status(CardStatus.ACTIVE)
                .balance(balance)
                .user(user)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
        return cardRepository.save(card);
    }

    private Transaction createTestTransaction(Card fromCard, Card toCard, BigDecimal amount, String description) {
        Transaction transaction = Transaction.builder()
                .fromCard(fromCard)
                .toCard(toCard)
                .amount(amount)
                .description(description)
                .status(TransactionStatus.COMPLETED)
                .transactionDate(LocalDateTime.now())
                .completedAt(LocalDateTime.now())
                .build();

        return transactionRepository.save(transaction);
    }
}

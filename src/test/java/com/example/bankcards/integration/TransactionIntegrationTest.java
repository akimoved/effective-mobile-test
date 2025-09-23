package com.example.bankcards.integration;

import com.example.bankcards.dto.request.CardCreateRequest;
import com.example.bankcards.dto.request.TransactionCreateRequest;
import com.example.bankcards.dto.response.CardResponse;
import com.example.bankcards.dto.response.TransactionResponse;
import com.example.bankcards.repository.CardRepository;
import com.example.bankcards.repository.TransactionRepository;
import com.example.bankcards.repository.UserRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.math.BigDecimal;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class TransactionIntegrationTest extends BaseIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private CardRepository cardRepository;

    @Autowired
    private TransactionRepository transactionRepository;

    @Autowired
    private UserRepository userRepository;

    private String userToken;
    private Long fromCardId;
    private Long toCardId;

    @BeforeEach
    void setUp() throws Exception {
        // Очистка данных
        transactionRepository.deleteAll();
        cardRepository.deleteAll();
        userRepository.deleteAll();

        // Регистрация пользователя
        String registerRequest = """
            {
                "username": "transactionuser",
                "email": "transaction@example.com",
                "password": "password123",
                "firstName": "Transaction",
                "lastName": "User"
            }
            """;

        MvcResult registerResult = mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(registerRequest))
                .andExpect(status().isOk())
                .andReturn();

        userToken = objectMapper.readTree(registerResult.getResponse().getContentAsString())
                .get("token").asText();

        // Создаем первую карту с начальным балансом
        CardCreateRequest card1Request = new CardCreateRequest("1111222233334444", "Transaction User");

        MvcResult card1Result = mockMvc.perform(post("/api/cards")
                        .header("Authorization", "Bearer " + userToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(card1Request)))
                .andExpect(status().isCreated())
                .andReturn();

        CardResponse card1Response = objectMapper.readValue(
                card1Result.getResponse().getContentAsString(),
                CardResponse.class
        );
        fromCardId = card1Response.id();

        // Создаем вторую карту
        CardCreateRequest card2Request = new CardCreateRequest("5555666677778888", "Transaction User");

        MvcResult card2Result = mockMvc.perform(post("/api/cards")
                        .header("Authorization", "Bearer " + userToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(card2Request)))
                .andExpect(status().isCreated())
                .andReturn();

        CardResponse card2Response = objectMapper.readValue(
                card2Result.getResponse().getContentAsString(),
                CardResponse.class
        );
        toCardId = card2Response.id();


    }

    @Test
    @DisplayName("Создание перевода между картами - успешный сценарий")
    void createTransfer_ShouldCreateTransaction() throws Exception {
        // Сначала пополняем карту (имитация)
        // В реальном приложении используйте сервис для пополнения баланса

        TransactionCreateRequest request = new TransactionCreateRequest(
                fromCardId, toCardId, new BigDecimal("100.00"), "Test transfer"
        );

        mockMvc.perform(post("/api/transactions")
                        .header("Authorization", "Bearer " + userToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.fromCardId").value(fromCardId))
                .andExpect(jsonPath("$.toCardId").value(toCardId))
                .andExpect(jsonPath("$.amount").value(100.00))
                .andExpect(jsonPath("$.status").value("COMPLETED"));
    }

    @Test
    @DisplayName("Создание перевода с недостаточным балансом - ошибка")
    void createTransfer_InsufficientFunds_ShouldReturnError() throws Exception {
        TransactionCreateRequest request = new TransactionCreateRequest(
                fromCardId, toCardId, new BigDecimal("1000000.00"), "Large transfer"
        );

        mockMvc.perform(post("/api/transactions")
                        .header("Authorization", "Bearer " + userToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Получение транзакции по ID")
    void getTransaction_ShouldReturnTransaction() throws Exception {
        // Сначала создаем транзакцию
        TransactionCreateRequest request = new TransactionCreateRequest(
                fromCardId, toCardId, new BigDecimal("50.00"), "Test transaction"
        );

        MvcResult createResult = mockMvc.perform(post("/api/transactions")
                        .header("Authorization", "Bearer " + userToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andReturn();

        TransactionResponse transactionResponse = objectMapper.readValue(
                createResult.getResponse().getContentAsString(),
                TransactionResponse.class
        );

        // Получаем созданную транзакцию
        mockMvc.perform(get("/api/transactions/" + transactionResponse.id())
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(transactionResponse.id()))
                .andExpect(jsonPath("$.amount").value(50.00))
                .andExpect(jsonPath("$.status").value("COMPLETED"));
    }

    @Test
    @DisplayName("Получение истории переводов пользователя")
    void getUserTransactions_ShouldReturnTransactions() throws Exception {
        // Создаем несколько транзакций
        for (int i = 0; i < 3; i++) {
            TransactionCreateRequest request = new TransactionCreateRequest(
                    fromCardId, toCardId, new BigDecimal("10.00"), "Transaction " + (i + 1)
            );

            mockMvc.perform(post("/api/transactions")
                            .header("Authorization", "Bearer " + userToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isCreated());
        }

        // Получаем историю
        mockMvc.perform(get("/api/transactions")
                        .header("Authorization", "Bearer " + userToken)
                        .param("page", "0")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content.length()").value(3));
    }

    @Test
    @DisplayName("Получение переводов конкретной карты")
    void getCardTransactions_ShouldReturnCardTransactions() throws Exception {
        // Создаем транзакцию
        TransactionCreateRequest request = new TransactionCreateRequest(
                fromCardId, toCardId, new BigDecimal("25.00"), "Card transaction"
        );

        mockMvc.perform(post("/api/transactions")
                        .header("Authorization", "Bearer " + userToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated());

        // Получаем транзакции карты
        mockMvc.perform(get("/api/transactions/card/" + fromCardId)
                        .header("Authorization", "Bearer " + userToken)
                        .param("page", "0")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content.length()").value(1));
    }

    @Test
    @DisplayName("Получение внутренних переводов")
    void getInternalTransfers_ShouldReturnInternalTransactions() throws Exception {
        // Создаем внутренний перевод
        TransactionCreateRequest request = new TransactionCreateRequest(
                fromCardId, toCardId, new BigDecimal("15.00"), "Internal transfer"
        );

        mockMvc.perform(post("/api/transactions")
                        .header("Authorization", "Bearer " + userToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated());

        // Получаем внутренние переводы
        mockMvc.perform(get("/api/transactions/internal")
                        .header("Authorization", "Bearer " + userToken)
                        .param("page", "0")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content.length()").value(1));
    }

    @Test
    @DisplayName("Получение баланса карты")
    void getCardBalance_ShouldReturnBalance() throws Exception {
        mockMvc.perform(get("/api/transactions/balance/" + fromCardId)
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.cardId").value(fromCardId))
                .andExpect(jsonPath("$.balance").exists());
    }

    @Test
    @DisplayName("Получение балансов всех карт пользователя")
    void getUserBalances_ShouldReturnAllBalances() throws Exception {
        mockMvc.perform(get("/api/transactions/balances")
                        .header("Authorization", "Bearer " + userToken)
                        .param("page", "0")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content.length()").value(2));
    }

    @Test
    @DisplayName("Попытка перевода на ту же карту - ошибка")
    void createTransfer_SameCard_ShouldReturnError() throws Exception {
        TransactionCreateRequest request = new TransactionCreateRequest(
                fromCardId, fromCardId, new BigDecimal("10.00"), "Same card transfer"
        );

        mockMvc.perform(post("/api/transactions")
                        .header("Authorization", "Bearer " + userToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Попытка перевода с заблокированной карты - ошибка")
    void createTransfer_BlockedCard_ShouldReturnError() throws Exception {
        // Блокируем карту
        mockMvc.perform(post("/api/cards/" + fromCardId + "/block")
                .header("Authorization", "Bearer " + userToken));

        TransactionCreateRequest request = new TransactionCreateRequest(
                fromCardId, toCardId, new BigDecimal("10.00"), "Blocked card transfer"
        );

        mockMvc.perform(post("/api/transactions")
                        .header("Authorization", "Bearer " + userToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }
}

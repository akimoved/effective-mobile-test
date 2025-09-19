package com.example.bankcards.service;

import com.example.bankcards.dto.response.BalanceResponse;
import com.example.bankcards.dto.request.TransactionCreateRequest;
import com.example.bankcards.dto.response.TransactionResponse;
import com.example.bankcards.entity.Card;
import com.example.bankcards.entity.enums.CardStatus;
import com.example.bankcards.entity.Transaction;
import com.example.bankcards.entity.enums.RoleName;
import com.example.bankcards.entity.enums.TransactionStatus;
import com.example.bankcards.entity.User;
import com.example.bankcards.exception.CardNotFoundException;
import com.example.bankcards.exception.InsufficientFundsException;
import com.example.bankcards.exception.InvalidTransactionException;
import com.example.bankcards.exception.TransactionNotFoundException;
import com.example.bankcards.repository.CardRepository;
import com.example.bankcards.repository.TransactionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Сервис для управления транзакциями (переводами между картами)
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class TransactionService {

    private final TransactionRepository transactionRepository;
    private final CardRepository cardRepository;
    private final UserService userService;
    private final EncryptionService encryptionService;

    /**
     * Создание перевода между картами
     */
    public TransactionResponse createTransfer(TransactionCreateRequest request, String username) {
        log.info("Создание перевода от пользователя: {} с карты {} на карту {} на сумму {}", 
                username, request.fromCardId(), request.toCardId(), request.amount());

        // Получаем пользователя
        User user = userService.findByUsername(username);

        // Получаем карты
        Card fromCard = findCardById(request.fromCardId());
        Card toCard = findCardById(request.toCardId());

        // Валидация перевода
        validateTransfer(fromCard, toCard, request.amount(), user);

        // Создаем транзакцию
        Transaction transaction = Transaction.builder()
                .fromCard(fromCard)
                .toCard(toCard)
                .amount(request.amount())
                .description(request.description())
                .status(TransactionStatus.PENDING)
                .transactionDate(LocalDateTime.now())
                .build();

        Transaction savedTransaction = transactionRepository.save(transaction);

        try {
            // Выполняем перевод
            executeTransfer(fromCard, toCard, request.amount());
            
            // Помечаем транзакцию как завершенную
            savedTransaction.markAsCompleted();
            savedTransaction = transactionRepository.save(savedTransaction);

            log.info("Перевод успешно выполнен: {}", savedTransaction.getTransactionInfo());

        } catch (Exception e) {
            // В случае ошибки помечаем транзакцию как неудачную
            savedTransaction.markAsFailed("Ошибка выполнения перевода: " + e.getMessage());
            transactionRepository.save(savedTransaction);
            
            log.error("Ошибка выполнения перевода: {}", e.getMessage(), e);
            throw new InvalidTransactionException("Не удалось выполнить перевод: " + e.getMessage());
        }

        return mapToResponse(savedTransaction);
    }

    /**
     * Получение транзакции по ID
     */
    @Transactional(readOnly = true)
    public TransactionResponse getTransaction(Long transactionId, String username) {
        log.debug("Получение транзакции с ID: {} для пользователя: {}", transactionId, username);

        Transaction transaction = findTransactionById(transactionId);
        validateTransactionAccess(transaction, username);

        return mapToResponse(transaction);
    }

    /**
     * Получение истории переводов пользователя
     */
    @Transactional(readOnly = true)
    public Page<TransactionResponse> getUserTransactions(String username, Pageable pageable) {
        log.debug("Получение истории переводов пользователя: {}", username);

        User user = userService.findByUsername(username);
        Page<Transaction> transactions = transactionRepository.findByUserId(user.getId(), pageable);

        return transactions.map(this::mapToResponse);
    }

    /**
     * Получение переводов конкретной карты
     */
    @Transactional(readOnly = true)
    public Page<TransactionResponse> getCardTransactions(Long cardId, String username, Pageable pageable) {
        log.debug("Получение переводов карты с ID: {} для пользователя: {}", cardId, username);

        Card card = findCardById(cardId);
        validateCardAccess(card, username);

        Page<Transaction> transactions = transactionRepository.findByCardId(cardId, pageable);
        return transactions.map(this::mapToResponse);
    }

    /**
     * Получение внутренних переводов пользователя (между своими картами)
     */
    @Transactional(readOnly = true)
    public Page<TransactionResponse> getInternalTransfers(String username, Pageable pageable) {
        log.debug("Получение внутренних переводов пользователя: {}", username);

        User user = userService.findByUsername(username);
        Page<Transaction> transactions = transactionRepository.findInternalTransfersByUserId(user.getId(), pageable);

        return transactions.map(this::mapToResponse);
    }

    /**
     * Получение баланса карты
     */
    @Transactional(readOnly = true)
    public BalanceResponse getCardBalance(Long cardId, String username) {
        log.debug("Получение баланса карты с ID: {} для пользователя: {}", cardId, username);

        Card card = findCardById(cardId);
        validateCardAccess(card, username);

        // Расшифровываем и маскируем номер карты
        String decryptedCardNumber = encryptionService.decrypt(card.getCardNumber());
        String maskedCardNumber = encryptionService.maskCardNumber(decryptedCardNumber);
        
        return new BalanceResponse(
                card.getId(),
                maskedCardNumber,
                card.getBalance(),
                card.getCardHolderName()
        );
    }

    /**
     * Получение всех балансов карт пользователя
     */
    @Transactional(readOnly = true)
    public Page<BalanceResponse> getUserBalances(String username, Pageable pageable) {
        log.debug("Получение балансов карт пользователя: {}", username);

        User user = userService.findByUsername(username);
        Page<Card> cards = cardRepository.findByUserIdOrderByCreatedAtDesc(user.getId(), pageable);

        return cards.map(card -> {
            // Расшифровываем и маскируем номер карты
            String decryptedCardNumber = encryptionService.decrypt(card.getCardNumber());
            String maskedCardNumber = encryptionService.maskCardNumber(decryptedCardNumber);
            
            return new BalanceResponse(
                    card.getId(),
                    maskedCardNumber,
                    card.getBalance(),
                    card.getCardHolderName()
            );
        });
    }

    /**
     * Получение всех транзакций (только для администраторов)
     */
    @Transactional(readOnly = true)
    public Page<TransactionResponse> getAllTransactions(String username, Pageable pageable) {
        log.info("Получение всех транзакций администратором: {}", username);

        User user = userService.findByUsername(username);
        if (!userService.hasRole(user.getId(), RoleName.ROLE_ADMIN)) {
            throw new AccessDeniedException("Недостаточно прав для выполнения операции");
        }

        Page<Transaction> transactions = transactionRepository.findAll(pageable);
        return transactions.map(this::mapToResponse);
    }

    /**
     * Валидация перевода
     */
    private void validateTransfer(Card fromCard, Card toCard, BigDecimal amount, User user) {
        // Проверяем, что обе карты принадлежат пользователю (только внутренние переводы)
        if (!fromCard.getUser().getId().equals(user.getId())) {
            throw new AccessDeniedException("Карта отправителя не принадлежит пользователю");
        }
        
        if (!toCard.getUser().getId().equals(user.getId())) {
            throw new InvalidTransactionException("Переводы возможны только между собственными картами");
        }

        // Проверяем, что карты разные
        if (fromCard.getId().equals(toCard.getId())) {
            throw new InvalidTransactionException("Нельзя переводить средства на ту же карту");
        }

        // Проверяем статус карт
        if (fromCard.getStatus() != CardStatus.ACTIVE) {
            throw new InvalidTransactionException("Карта отправителя не активна");
        }

        if (toCard.getStatus() != CardStatus.ACTIVE) {
            throw new InvalidTransactionException("Карта получателя не активна");
        }

        // Проверяем срок действия карт
        if (fromCard.isExpired()) {
            throw new InvalidTransactionException("Срок действия карты отправителя истек");
        }

        if (toCard.isExpired()) {
            throw new InvalidTransactionException("Срок действия карты получателя истек");
        }

        // Проверяем достаточность средств
        if (fromCard.getBalance().compareTo(amount) < 0) {
            // Расшифровываем и маскируем номер карты для сообщения об ошибке
            String decryptedCardNumber = encryptionService.decrypt(fromCard.getCardNumber());
            String maskedCardNumber = encryptionService.maskCardNumber(decryptedCardNumber);
            
            throw new InsufficientFundsException(
                String.format("Недостаточно средств на карте %s. Доступно: %s, требуется: %s", 
                    maskedCardNumber, fromCard.getBalance(), amount)
            );
        }

        // Проверяем минимальную сумму перевода
        if (amount.compareTo(new BigDecimal("0.01")) < 0) {
            throw new InvalidTransactionException("Минимальная сумма перевода: 0.01");
        }

        // Проверяем максимальную сумму перевода (например, 1,000,000)
        if (amount.compareTo(new BigDecimal("1000000")) > 0) {
            throw new InvalidTransactionException("Максимальная сумма перевода: 1,000,000");
        }
    }

    /**
     * Выполнение перевода (обновление балансов)
     */
    private void executeTransfer(Card fromCard, Card toCard, BigDecimal amount) {
        // Списываем с карты отправителя
        if (!fromCard.deductBalance(amount)) {
            throw new InsufficientFundsException("Не удалось списать средства с карты отправителя");
        }

        // Зачисляем на карту получателя
        toCard.addBalance(amount);

        // Обновляем время изменения карт
        fromCard.setUpdatedAt(LocalDateTime.now());
        toCard.setUpdatedAt(LocalDateTime.now());

        // Сохраняем изменения
        cardRepository.save(fromCard);
        cardRepository.save(toCard);

        // Расшифровываем и маскируем номера карт для логирования
        String fromCardDecrypted = encryptionService.decrypt(fromCard.getCardNumber());
        String toCardDecrypted = encryptionService.decrypt(toCard.getCardNumber());
        String fromCardMasked = encryptionService.maskCardNumber(fromCardDecrypted);
        String toCardMasked = encryptionService.maskCardNumber(toCardDecrypted);
        
        log.debug("Перевод выполнен: {} -> {}, сумма: {}", 
                fromCardMasked, toCardMasked, amount);
    }

    /**
     * Поиск карты по ID
     */
    private Card findCardById(Long cardId) {
        return cardRepository.findById(cardId)
                .orElseThrow(() -> new CardNotFoundException("Карта с ID " + cardId + " не найдена"));
    }

    /**
     * Поиск транзакции по ID
     */
    private Transaction findTransactionById(Long transactionId) {
        return transactionRepository.findById(transactionId)
                .orElseThrow(() -> new TransactionNotFoundException("Транзакция с ID " + transactionId + " не найдена"));
    }

    /**
     * Проверка доступа к карте
     */
    private void validateCardAccess(Card card, String username) {
        if (!card.getUser().getUsername().equals(username)) {
            User user = userService.findByUsername(username);
            if (!userService.hasRole(user.getId(), RoleName.ROLE_ADMIN)) {
                throw new AccessDeniedException("Недостаточно прав для доступа к карте");
            }
        }
    }

    /**
     * Проверка доступа к транзакции
     */
    private void validateTransactionAccess(Transaction transaction, String username) {
        User user = userService.findByUsername(username);
        
        // Пользователь может видеть транзакции, где участвуют его карты
        boolean hasAccess = transaction.getFromCard().getUser().getId().equals(user.getId()) ||
                           transaction.getToCard().getUser().getId().equals(user.getId());

        if (!hasAccess && !userService.hasRole(user.getId(), RoleName.ROLE_ADMIN)) {
            throw new AccessDeniedException("Недостаточно прав для доступа к транзакции");
        }
    }

    /**
     * Преобразование Transaction в TransactionResponse
     */
    private TransactionResponse mapToResponse(Transaction transaction) {
        // Расшифровываем и маскируем номера карт
        String fromCardDecrypted = encryptionService.decrypt(transaction.getFromCard().getCardNumber());
        String toCardDecrypted = encryptionService.decrypt(transaction.getToCard().getCardNumber());
        String fromCardMasked = encryptionService.maskCardNumber(fromCardDecrypted);
        String toCardMasked = encryptionService.maskCardNumber(toCardDecrypted);
        
        return new TransactionResponse(
                transaction.getId(),
                transaction.getFromCard().getId(),
                fromCardMasked,
                transaction.getToCard().getId(),
                toCardMasked,
                transaction.getAmount(),
                transaction.getDescription(),
                transaction.getStatus(),
                transaction.getTransactionDate(),
                transaction.getCompletedAt(),
                transaction.getErrorMessage()
        );
    }
}

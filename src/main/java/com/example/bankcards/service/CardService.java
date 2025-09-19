package com.example.bankcards.service;

import com.example.bankcards.dto.request.CardCreateRequest;
import com.example.bankcards.dto.response.CardResponse;
import com.example.bankcards.dto.request.CardUpdateRequest;
import com.example.bankcards.entity.Card;
import com.example.bankcards.entity.enums.CardStatus;
import com.example.bankcards.entity.User;
import com.example.bankcards.entity.enums.RoleName;
import com.example.bankcards.exception.CardNotFoundException;
import com.example.bankcards.exception.DuplicateCardNumberException;
import com.example.bankcards.repository.CardRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * Сервис для управления банковскими картами
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class CardService {

    private final CardRepository cardRepository;
    private final UserService userService;
    private final EncryptionService encryptionService;

    /**
     * Создание новой карты
     */
    public CardResponse createCard(CardCreateRequest request, String username) {
        log.info("Создание карты для пользователя: {}", username);

        User user = userService.findByUsername(username);

        // Шифруем номер карты для проверки дубликатов
        String encryptedCardNumber = encryptionService.encrypt(request.cardNumber());
        
        // Проверяем дубликаты по зашифрованному номеру
        if (cardRepository.existsByCardNumber(encryptedCardNumber)) {
            throw new DuplicateCardNumberException("Карта с номером " + encryptionService.maskCardNumber(request.cardNumber()) + " уже существует");
        }

        Card card = Card.builder()
                .cardNumber(encryptedCardNumber) // Сохраняем зашифрованный номер
                .cardHolderName(request.cardholderName())
                .expiryDate(java.time.LocalDate.now().plusYears(3)) // Карта действительна 3 года
                .status(CardStatus.ACTIVE)
                .user(user)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        Card savedCard = cardRepository.save(card);
        log.info("Карта создана с ID: {} для пользователя: {}", savedCard.getId(), username);

        return mapToResponse(savedCard);
    }

    /**
     * Получение карты по ID
     */
    @Transactional(readOnly = true)
    public CardResponse getCard(Long cardId, String username) {
        log.debug("Получение карты с ID: {} для пользователя: {}", cardId, username);

        Card card = findCardById(cardId);
        validateCardAccess(card, username);

        return mapToResponse(card);
    }

    /**
     * Получение всех карт пользователя
     */
    @Transactional(readOnly = true)
    public Page<CardResponse> getUserCards(String username, Pageable pageable) {
        log.debug("Получение карт пользователя: {}", username);

        User user = userService.findByUsername(username);
        Page<Card> cards = cardRepository.findByUserIdOrderByCreatedAtDesc(user.getId(), pageable);

        return cards.map(this::mapToResponse);
    }

    /**
     * Обновление карты
     */
    public CardResponse updateCard(Long cardId, CardUpdateRequest request, String username) {
        log.info("Обновление карты с ID: {} для пользователя: {}", cardId, username);

        Card card = findCardById(cardId);
        validateCardAccess(card, username);

        if (request.cardholderName() != null) {
            card.setCardHolderName(request.cardholderName());
        }

        if (request.status() != null) {
            card.setStatus(request.status());
        }

        card.setUpdatedAt(LocalDateTime.now());
        Card updatedCard = cardRepository.save(card);

        log.info("Карта с ID: {} обновлена", cardId);
        return mapToResponse(updatedCard);
    }

    /**
     * Блокировка карты
     */
    public CardResponse blockCard(Long cardId, String username) {
        log.info("Блокировка карты с ID: {} для пользователя: {}", cardId, username);

        Card card = findCardById(cardId);
        validateCardAccess(card, username);

        card.setStatus(CardStatus.BLOCKED);
        card.setUpdatedAt(LocalDateTime.now());
        Card blockedCard = cardRepository.save(card);

        log.info("Карта с ID: {} заблокирована", cardId);
        return mapToResponse(blockedCard);
    }

    /**
     * Разблокировка карты
     */
    public CardResponse unblockCard(Long cardId, String username) {
        log.info("Разблокировка карты с ID: {} для пользователя: {}", cardId, username);

        Card card = findCardById(cardId);
        validateCardAccess(card, username);

        card.setStatus(CardStatus.ACTIVE);
        card.setUpdatedAt(LocalDateTime.now());
        Card unblockedCard = cardRepository.save(card);

        log.info("Карта с ID: {} разблокирована", cardId);
        return mapToResponse(unblockedCard);
    }

    /**
     * Удаление карты
     */
    public void deleteCard(Long cardId, String username) {
        log.info("Удаление карты с ID: {} для пользователя: {}", cardId, username);

        Card card = findCardById(cardId);
        validateCardAccess(card, username);

        cardRepository.delete(card);
        log.info("Карта с ID: {} удалена", cardId);
    }

    /**
     * Поиск карты по номеру (только для администраторов)
     */
    @Transactional(readOnly = true)
    public CardResponse findByCardNumber(String cardNumber, String username) {
        log.info("Поиск карты по номеру для администратора: {}", username);

        User user = userService.findByUsername(username);
        if (!userService.hasRole(user.getId(), RoleName.ROLE_ADMIN)) {
            throw new AccessDeniedException("Недостаточно прав для выполнения операции");
        }

        // Шифруем входящий номер карты для поиска
        String encryptedCardNumber = encryptionService.encrypt(cardNumber);
        Card card = cardRepository.findByCardNumber(encryptedCardNumber)
                .orElseThrow(() -> new CardNotFoundException("Карта с номером " + encryptionService.maskCardNumber(cardNumber) + " не найдена"));

        return mapToResponse(card);
    }

    /**
     * Получение всех карт (только для администраторов)
     */
    @Transactional(readOnly = true)
    public Page<CardResponse> getAllCards(String username, Pageable pageable) {
        log.info("Получение всех карт для администратора: {}", username);

        User user = userService.findByUsername(username);
        if (!userService.hasRole(user.getId(), RoleName.ROLE_ADMIN)) {
            throw new AccessDeniedException("Недостаточно прав для выполнения операции");
        }

        Page<Card> cards = cardRepository.findAllByOrderByCreatedAtDesc(pageable);
        return cards.map(this::mapToResponse);
    }

    /**
     * Поиск карты по ID
     */
    private Card findCardById(Long cardId) {
        return cardRepository.findById(cardId)
                .orElseThrow(() -> new CardNotFoundException("Карта с ID " + cardId + " не найдена"));
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
     * Преобразование Card в CardResponse
     */
    private CardResponse mapToResponse(Card card) {
        // Расшифровываем номер карты и маскируем его для безопасности
        String decryptedCardNumber = encryptionService.decrypt(card.getCardNumber());
        String maskedCardNumber = encryptionService.maskCardNumber(decryptedCardNumber);
        
        return new CardResponse(
                card.getId(),
                maskedCardNumber,
                card.getCardHolderName(),
                card.getStatus(),
                card.getCreatedAt(),
                card.getUpdatedAt()
        );
    }
}

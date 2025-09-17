package com.example.bankcards.controller;

import com.example.bankcards.dto.request.CardCreateRequest;
import com.example.bankcards.dto.response.CardResponse;
import com.example.bankcards.dto.request.CardUpdateRequest;
import com.example.bankcards.service.CardService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

/**
 * Контроллер для управления банковскими картами
 */
@RestController
@RequestMapping("/api/cards")
@RequiredArgsConstructor
@Slf4j
public class CardController {

    private final CardService cardService;

    /**
     * Создание новой карты
     */
    @PostMapping
    public ResponseEntity<CardResponse> createCard(
            @Valid @RequestBody CardCreateRequest request,
            Authentication authentication) {
        
        log.info("Запрос на создание карты от пользователя: {}", authentication.getName());
        
        CardResponse response = cardService.createCard(request, authentication.getName());
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * Получение карты по ID
     */
    @GetMapping("/{cardId}")
    public ResponseEntity<CardResponse> getCard(
            @PathVariable Long cardId,
            Authentication authentication) {
        
        log.debug("Запрос карты с ID: {} от пользователя: {}", cardId, authentication.getName());
        
        CardResponse response = cardService.getCard(cardId, authentication.getName());
        return ResponseEntity.ok(response);
    }

    /**
     * Получение всех карт текущего пользователя
     */
    @GetMapping
    public ResponseEntity<Page<CardResponse>> getUserCards(
            @PageableDefault(size = 10) Pageable pageable,
            Authentication authentication) {
        
        log.debug("Запрос карт пользователя: {}", authentication.getName());
        
        Page<CardResponse> response = cardService.getUserCards(authentication.getName(), pageable);
        return ResponseEntity.ok(response);
    }

    /**
     * Обновление карты
     */
    @PutMapping("/{cardId}")
    public ResponseEntity<CardResponse> updateCard(
            @PathVariable Long cardId,
            @Valid @RequestBody CardUpdateRequest request,
            Authentication authentication) {
        
        log.info("Запрос на обновление карты с ID: {} от пользователя: {}", cardId, authentication.getName());
        
        CardResponse response = cardService.updateCard(cardId, request, authentication.getName());
        return ResponseEntity.ok(response);
    }

    /**
     * Блокировка карты
     */
    @PostMapping("/{cardId}/block")
    public ResponseEntity<CardResponse> blockCard(
            @PathVariable Long cardId,
            Authentication authentication) {
        
        log.info("Запрос на блокировку карты с ID: {} от пользователя: {}", cardId, authentication.getName());
        
        CardResponse response = cardService.blockCard(cardId, authentication.getName());
        return ResponseEntity.ok(response);
    }

    /**
     * Разблокировка карты
     */
    @PostMapping("/{cardId}/unblock")
    public ResponseEntity<CardResponse> unblockCard(
            @PathVariable Long cardId,
            Authentication authentication) {
        
        log.info("Запрос на разблокировку карты с ID: {} от пользователя: {}", cardId, authentication.getName());
        
        CardResponse response = cardService.unblockCard(cardId, authentication.getName());
        return ResponseEntity.ok(response);
    }

    /**
     * Удаление карты
     */
    @DeleteMapping("/{cardId}")
    public ResponseEntity<Void> deleteCard(
            @PathVariable Long cardId,
            Authentication authentication) {
        
        log.info("Запрос на удаление карты с ID: {} от пользователя: {}", cardId, authentication.getName());
        
        cardService.deleteCard(cardId, authentication.getName());
        return ResponseEntity.noContent().build();
    }

    /**
     * Поиск карты по номеру (только для администраторов)
     */
    @GetMapping("/search")
    public ResponseEntity<CardResponse> findByCardNumber(
            @RequestParam String cardNumber,
            Authentication authentication) {
        
        log.info("Поиск карты по номеру администратором: {}", authentication.getName());
        
        CardResponse response = cardService.findByCardNumber(cardNumber, authentication.getName());
        return ResponseEntity.ok(response);
    }

    /**
     * Получение всех карт (только для администраторов)
     */
    @GetMapping("/admin/all")
    public ResponseEntity<Page<CardResponse>> getAllCards(
            @PageableDefault(size = 20) Pageable pageable,
            Authentication authentication) {
        
        log.info("Запрос всех карт администратором: {}", authentication.getName());
        
        Page<CardResponse> response = cardService.getAllCards(authentication.getName(), pageable);
        return ResponseEntity.ok(response);
    }
}

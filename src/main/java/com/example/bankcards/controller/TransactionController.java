package com.example.bankcards.controller;

import com.example.bankcards.dto.response.BalanceResponse;
import com.example.bankcards.dto.request.TransactionCreateRequest;
import com.example.bankcards.dto.response.TransactionResponse;
import com.example.bankcards.service.TransactionService;
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
 * Контроллер для управления транзакциями (переводами)
 */
@RestController
@RequestMapping("/api/transactions")
@RequiredArgsConstructor
@Slf4j
public class TransactionController {

    private final TransactionService transactionService;

    /**
     * Создание перевода между картами
     */
    @PostMapping
    public ResponseEntity<TransactionResponse> createTransfer(
            @Valid @RequestBody TransactionCreateRequest request,
            Authentication authentication) {
        
        log.info("Запрос на создание перевода от пользователя: {}", authentication.getName());
        
        TransactionResponse response = transactionService.createTransfer(request, authentication.getName());
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * Получение транзакции по ID
     */
    @GetMapping("/{transactionId}")
    public ResponseEntity<TransactionResponse> getTransaction(
            @PathVariable Long transactionId,
            Authentication authentication) {
        
        log.debug("Запрос транзакции с ID: {} от пользователя: {}", transactionId, authentication.getName());
        
        TransactionResponse response = transactionService.getTransaction(transactionId, authentication.getName());
        return ResponseEntity.ok(response);
    }

    /**
     * Получение истории переводов пользователя
     */
    @GetMapping
    public ResponseEntity<Page<TransactionResponse>> getUserTransactions(
            @PageableDefault(size = 10) Pageable pageable,
            Authentication authentication) {
        
        log.debug("Запрос истории переводов пользователя: {}", authentication.getName());
        
        Page<TransactionResponse> response = transactionService.getUserTransactions(authentication.getName(), pageable);
        return ResponseEntity.ok(response);
    }

    /**
     * Получение переводов конкретной карты
     */
    @GetMapping("/card/{cardId}")
    public ResponseEntity<Page<TransactionResponse>> getCardTransactions(
            @PathVariable Long cardId,
            @PageableDefault(size = 10) Pageable pageable,
            Authentication authentication) {
        
        log.debug("Запрос переводов карты с ID: {} от пользователя: {}", cardId, authentication.getName());
        
        Page<TransactionResponse> response = transactionService.getCardTransactions(cardId, authentication.getName(), pageable);
        return ResponseEntity.ok(response);
    }

    /**
     * Получение внутренних переводов пользователя (между своими картами)
     */
    @GetMapping("/internal")
    public ResponseEntity<Page<TransactionResponse>> getInternalTransfers(
            @PageableDefault(size = 10) Pageable pageable,
            Authentication authentication) {
        
        log.debug("Запрос внутренних переводов пользователя: {}", authentication.getName());
        
        Page<TransactionResponse> response = transactionService.getInternalTransfers(authentication.getName(), pageable);
        return ResponseEntity.ok(response);
    }

    /**
     * Получение баланса карты
     */
    @GetMapping("/balance/{cardId}")
    public ResponseEntity<BalanceResponse> getCardBalance(
            @PathVariable Long cardId,
            Authentication authentication) {
        
        log.debug("Запрос баланса карты с ID: {} от пользователя: {}", cardId, authentication.getName());
        
        BalanceResponse response = transactionService.getCardBalance(cardId, authentication.getName());
        return ResponseEntity.ok(response);
    }

    /**
     * Получение балансов всех карт пользователя
     */
    @GetMapping("/balances")
    public ResponseEntity<Page<BalanceResponse>> getUserBalances(
            @PageableDefault(size = 10) Pageable pageable,
            Authentication authentication) {
        
        log.debug("Запрос балансов карт пользователя: {}", authentication.getName());
        
        Page<BalanceResponse> response = transactionService.getUserBalances(authentication.getName(), pageable);
        return ResponseEntity.ok(response);
    }

    /**
     * Получение всех транзакций (только для администраторов)
     */
    @GetMapping("/admin/all")
    public ResponseEntity<Page<TransactionResponse>> getAllTransactions(
            @PageableDefault(size = 20) Pageable pageable,
            Authentication authentication) {
        
        log.info("Запрос всех транзакций администратором: {}", authentication.getName());
        
        Page<TransactionResponse> response = transactionService.getAllTransactions(authentication.getName(), pageable);
        return ResponseEntity.ok(response);
    }
}

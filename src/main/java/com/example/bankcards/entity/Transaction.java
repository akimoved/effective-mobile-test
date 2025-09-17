package com.example.bankcards.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Сущность транзакции (перевода между картами)
 */
@Entity
@Table(name = "transactions")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class Transaction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "from_card_id", nullable = false)
    @NotNull
    @ToString.Exclude
    private Card fromCard;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "to_card_id", nullable = false)
    @NotNull
    @ToString.Exclude
    private Card toCard;

    @Column(name = "amount", nullable = false, precision = 19, scale = 2)
    @NotNull
    @DecimalMin(value = "0.01", inclusive = true)
    private BigDecimal amount;

    @Column(name = "description", length = 500)
    @Size(max = 500)
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    @NotNull
    @Builder.Default
    private TransactionStatus status = TransactionStatus.PENDING;

    @Column(name = "transaction_date", nullable = false, updatable = false)
    private LocalDateTime transactionDate;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    @Column(name = "error_message", length = 1000)
    @Size(max = 1000)
    private String errorMessage;

    // Дополнительный конструктор для удобства
    public Transaction(Card fromCard, Card toCard, BigDecimal amount, String description) {
        this.fromCard = fromCard;
        this.toCard = toCard;
        this.amount = amount;
        this.description = description;
        this.status = TransactionStatus.PENDING;
    }

    // Вспомогательные методы
    /**
     * Помечает транзакцию как завершенную
     */
    public void markAsCompleted() {
        this.status = TransactionStatus.COMPLETED;
        this.completedAt = LocalDateTime.now();
        this.errorMessage = null;
    }

    /**
     * Помечает транзакцию как неудачную
     */
    public void markAsFailed(String errorMessage) {
        this.status = TransactionStatus.FAILED;
        this.completedAt = LocalDateTime.now();
        this.errorMessage = errorMessage;
    }

    /**
     * Помечает транзакцию как отмененную
     */
    public void markAsCancelled(String reason) {
        this.status = TransactionStatus.CANCELLED;
        this.completedAt = LocalDateTime.now();
        this.errorMessage = reason;
    }

    /**
     * Проверяет, является ли транзакция внутренней (между картами одного пользователя)
     */
    public boolean isInternalTransfer() {
        return fromCard != null && toCard != null && 
               fromCard.getUser() != null && toCard.getUser() != null &&
               fromCard.getUser().getId().equals(toCard.getUser().getId());
    }

    /**
     * Проверяет, завершена ли транзакция
     */
    public boolean isCompleted() {
        return status == TransactionStatus.COMPLETED;
    }

    /**
     * Проверяет, находится ли транзакция в процессе обработки
     */
    public boolean isPending() {
        return status == TransactionStatus.PENDING;
    }

    /**
     * Возвращает маскированную информацию о картах для логирования
     * Использует стандартную маску для зашифрованных номеров карт
     */
    public String getTransactionInfo() {
        String fromCardMasked = fromCard != null ? "**** **** **** ****" : "null";
        String toCardMasked = toCard != null ? "**** **** **** ****" : "null";
        return String.format("Transaction{id=%d, from=%s, to=%s, amount=%s, status=%s, date=%s, completed=%s}",
                id, fromCardMasked, toCardMasked, amount, status, transactionDate, completedAt);
    }
}

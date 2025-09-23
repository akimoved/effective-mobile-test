package com.example.bankcards.entity;

import com.example.bankcards.entity.enums.CardStatus;
import jakarta.persistence.*;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

/**
 * Сущность банковской карты
 */
@Entity
@Table(name = "cards")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class Card {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "card_number", unique = true, nullable = false)
    @NotBlank
    @EqualsAndHashCode.Include
    private String cardNumber;

    @Column(name = "card_holder_name", nullable = false)
    @NotBlank
    @Size(max = 100)
    private String cardHolderName;

    @Column(name = "expiry_date", nullable = false)
    @NotNull
    private LocalDate expiryDate;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    @NotNull
    @Builder.Default
    private CardStatus status = CardStatus.ACTIVE;

    @Column(name = "balance", nullable = false, precision = 19, scale = 2)
    @NotNull
    @DecimalMin(value = "0.0")
    @Builder.Default
    private BigDecimal balance = BigDecimal.ZERO;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    @NotNull
    @ToString.Exclude
    private User user;

    @OneToMany(mappedBy = "fromCard", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @Builder.Default
    @ToString.Exclude
    private Set<Transaction> outgoingTransactions = new HashSet<>();

    @OneToMany(mappedBy = "toCard", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @Builder.Default
    @ToString.Exclude
    private Set<Transaction> incomingTransactions = new HashSet<>();

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    // Дополнительный конструктор для удобства
    public Card(String cardNumber, String cardHolderName, LocalDate expiryDate, User user) {
        this.cardNumber = cardNumber;
        this.cardHolderName = cardHolderName;
        this.expiryDate = expiryDate;
        this.user = user;
        this.status = CardStatus.ACTIVE;
        this.balance = BigDecimal.ZERO;
        this.outgoingTransactions = new HashSet<>();
        this.incomingTransactions = new HashSet<>();
    }

    // Вспомогательные методы
    /**
     * Проверяет, истек ли срок действия карты
     */
    public boolean isExpired() {
        return LocalDate.now().isAfter(expiryDate);
    }

    /**
     * Проверяет, активна ли карта
     */
    public boolean isActive() {
        return status == CardStatus.ACTIVE && !isExpired();
    }

    /**
     * Добавляет средства на карту
     */
    public void addBalance(BigDecimal amount) {
        if (amount.compareTo(BigDecimal.ZERO) > 0) {
            this.balance = this.balance.add(amount);
        }
    }

    /**
     * Списывает средства с карты
     */
    public boolean deductBalance(BigDecimal amount) {
        if (amount.compareTo(BigDecimal.ZERO) > 0 && this.balance.compareTo(amount) >= 0) {
            this.balance = this.balance.subtract(amount);
            return true;
        }
        return false;
    }
}

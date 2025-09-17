package com.example.bankcards.repository;

import com.example.bankcards.entity.Card;
import com.example.bankcards.entity.CardStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * Репозиторий для работы с банковскими картами
 */
@Repository
public interface CardRepository extends JpaRepository<Card, Long> {

    /**
     * Поиск карт пользователя с пагинацией
     */
    Page<Card> findByUserId(Long userId, Pageable pageable);

    /**
     * Поиск карт пользователя с сортировкой по дате создания
     */
    Page<Card> findByUserIdOrderByCreatedAtDesc(Long userId, Pageable pageable);

    /**
     * Поиск всех карт с сортировкой по дате создания
     */
    Page<Card> findAllByOrderByCreatedAtDesc(Pageable pageable);

    /**
     * Поиск карт пользователя по статусу
     */
    List<Card> findByUserIdAndStatus(Long userId, CardStatus status);

    /**
     * Поиск активных карт пользователя
     */
    @Query("SELECT c FROM Card c WHERE c.user.id = :userId AND c.status = 'ACTIVE' AND c.expiryDate > CURRENT_DATE")
    List<Card> findActiveCardsByUserId(@Param("userId") Long userId);

    /**
     * Поиск карты по номеру (зашифрованному)
     */
    Optional<Card> findByCardNumber(String cardNumber);

    /**
     * Фильтрация карт пользователя по статусам
     */
    Page<Card> findByUserIdAndStatusIn(Long userId, List<CardStatus> statuses, Pageable pageable);

    /**
     * Поиск карт по статусу (для администраторов)
     */
    Page<Card> findByStatus(CardStatus status, Pageable pageable);

    /**
     * Подсчет количества карт у пользователя
     */
    long countByUserId(Long userId);

    /**
     * Подсчет активных карт у пользователя
     */
    @Query("SELECT COUNT(c) FROM Card c WHERE c.user.id = :userId AND c.status = 'ACTIVE'")
    long countActiveCardsByUserId(@Param("userId") Long userId);

    /**
     * Поиск карт с истекающим сроком действия
     */
    @Query("SELECT c FROM Card c WHERE c.expiryDate BETWEEN :startDate AND :endDate AND c.status = 'ACTIVE'")
    List<Card> findCardsExpiringBetween(@Param("startDate") LocalDate startDate, @Param("endDate") LocalDate endDate);

    /**
     * Поиск карт с балансом больше указанной суммы
     */
    @Query("SELECT c FROM Card c WHERE c.user.id = :userId AND c.balance >= :minBalance")
    List<Card> findByUserIdAndBalanceGreaterThanEqual(@Param("userId") Long userId, @Param("minBalance") BigDecimal minBalance);

    /**
     * Поиск всех карт с фильтрацией по владельцу карты (для поиска)
     */
    @Query("SELECT c FROM Card c WHERE c.user.id = :userId AND " +
           "(LOWER(c.cardHolderName) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
           "c.cardNumber LIKE CONCAT('%', :search, '%'))")
    Page<Card> findByUserIdAndSearch(@Param("userId") Long userId, @Param("search") String search, Pageable pageable);

    /**
     * Административный поиск карт по различным критериям
     */
    @Query("SELECT c FROM Card c WHERE " +
           "(:status IS NULL OR c.status = :status) AND " +
           "(:userId IS NULL OR c.user.id = :userId) AND " +
           "(:search IS NULL OR LOWER(c.cardHolderName) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
           "c.cardNumber LIKE CONCAT('%', :search, '%'))")
    Page<Card> findCardsWithFilters(@Param("status") CardStatus status, 
                                   @Param("userId") Long userId, 
                                   @Param("search") String search, 
                                   Pageable pageable);

    /**
     * Проверка существования карты по номеру
     */
    boolean existsByCardNumber(String cardNumber);

    /**
     * Получение общего баланса всех активных карт пользователя
     */
    @Query("SELECT COALESCE(SUM(c.balance), 0) FROM Card c WHERE c.user.id = :userId AND c.status = 'ACTIVE'")
    BigDecimal getTotalBalanceByUserId(@Param("userId") Long userId);
}

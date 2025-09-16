package com.example.bankcards.repository;

import com.example.bankcards.entity.Transaction;
import com.example.bankcards.entity.TransactionStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Репозиторий для работы с транзакциями
 */
@Repository
public interface TransactionRepository extends JpaRepository<Transaction, Long> {

    /**
     * История переводов пользователя (входящие и исходящие)
     */
    @Query("SELECT t FROM Transaction t WHERE t.fromCard.user.id = :userId OR t.toCard.user.id = :userId ORDER BY t.transactionDate DESC")
    Page<Transaction> findByUserId(@Param("userId") Long userId, Pageable pageable);

    /**
     * Переводы конкретной карты (входящие и исходящие)
     */
    @Query("SELECT t FROM Transaction t WHERE t.fromCard.id = :cardId OR t.toCard.id = :cardId ORDER BY t.transactionDate DESC")
    Page<Transaction> findByCardId(@Param("cardId") Long cardId, Pageable pageable);

    /**
     * Исходящие переводы карты
     */
    Page<Transaction> findByFromCardIdOrderByTransactionDateDesc(Long fromCardId, Pageable pageable);

    /**
     * Входящие переводы карты
     */
    Page<Transaction> findByToCardIdOrderByTransactionDateDesc(Long toCardId, Pageable pageable);

    /**
     * Фильтрация переводов пользователя по статусу
     */
    @Query("SELECT t FROM Transaction t WHERE (t.fromCard.user.id = :userId OR t.toCard.user.id = :userId) " +
           "AND t.status = :status ORDER BY t.transactionDate DESC")
    Page<Transaction> findByUserIdAndStatus(@Param("userId") Long userId, @Param("status") TransactionStatus status, Pageable pageable);

    /**
     * Переводы между конкретными картами
     */
    @Query("SELECT t FROM Transaction t WHERE " +
           "(t.fromCard.id = :fromCardId AND t.toCard.id = :toCardId) OR " +
           "(t.fromCard.id = :toCardId AND t.toCard.id = :fromCardId) " +
           "ORDER BY t.transactionDate DESC")
    Page<Transaction> findTransactionsBetweenCards(@Param("fromCardId") Long fromCardId, 
                                                  @Param("toCardId") Long toCardId, 
                                                  Pageable pageable);

    /**
     * Переводы за период времени
     */
    @Query("SELECT t FROM Transaction t WHERE (t.fromCard.user.id = :userId OR t.toCard.user.id = :userId) " +
           "AND t.transactionDate BETWEEN :startDate AND :endDate ORDER BY t.transactionDate DESC")
    Page<Transaction> findByUserIdAndDateRange(@Param("userId") Long userId, 
                                              @Param("startDate") LocalDateTime startDate, 
                                              @Param("endDate") LocalDateTime endDate, 
                                              Pageable pageable);

    /**
     * Общая сумма исходящих переводов карты
     */
    @Query("SELECT COALESCE(SUM(t.amount), 0) FROM Transaction t WHERE t.fromCard.id = :cardId AND t.status = 'COMPLETED'")
    BigDecimal getTotalOutgoingAmount(@Param("cardId") Long cardId);

    /**
     * Общая сумма входящих переводов карты
     */
    @Query("SELECT COALESCE(SUM(t.amount), 0) FROM Transaction t WHERE t.toCard.id = :cardId AND t.status = 'COMPLETED'")
    BigDecimal getTotalIncomingAmount(@Param("cardId") Long cardId);

    /**
     * Статистика переводов пользователя за период
     */
    @Query("SELECT COALESCE(SUM(t.amount), 0) FROM Transaction t WHERE t.fromCard.user.id = :userId " +
           "AND t.status = 'COMPLETED' AND t.transactionDate BETWEEN :startDate AND :endDate")
    BigDecimal getTotalOutgoingAmountByUserAndPeriod(@Param("userId") Long userId, 
                                                    @Param("startDate") LocalDateTime startDate, 
                                                    @Param("endDate") LocalDateTime endDate);

    /**
     * Количество переводов пользователя за период
     */
    @Query("SELECT COUNT(t) FROM Transaction t WHERE (t.fromCard.user.id = :userId OR t.toCard.user.id = :userId) " +
           "AND t.transactionDate BETWEEN :startDate AND :endDate")
    long countTransactionsByUserAndPeriod(@Param("userId") Long userId, 
                                         @Param("startDate") LocalDateTime startDate, 
                                         @Param("endDate") LocalDateTime endDate);

    /**
     * Поиск незавершенных транзакций (для обработки)
     */
    List<Transaction> findByStatusAndTransactionDateBefore(TransactionStatus status, LocalDateTime dateTime);

    /**
     * Внутренние переводы пользователя (между своими картами)
     */
    @Query("SELECT t FROM Transaction t WHERE t.fromCard.user.id = :userId AND t.toCard.user.id = :userId " +
           "ORDER BY t.transactionDate DESC")
    Page<Transaction> findInternalTransfersByUserId(@Param("userId") Long userId, Pageable pageable);

    /**
     * Административный поиск всех транзакций с фильтрами
     */
    @Query("SELECT t FROM Transaction t WHERE " +
           "(:status IS NULL OR t.status = :status) AND " +
           "(:userId IS NULL OR t.fromCard.user.id = :userId OR t.toCard.user.id = :userId) AND " +
           "(:startDate IS NULL OR t.transactionDate >= :startDate) AND " +
           "(:endDate IS NULL OR t.transactionDate <= :endDate) " +
           "ORDER BY t.transactionDate DESC")
    Page<Transaction> findTransactionsWithFilters(@Param("status") TransactionStatus status,
                                                 @Param("userId") Long userId,
                                                 @Param("startDate") LocalDateTime startDate,
                                                 @Param("endDate") LocalDateTime endDate,
                                                 Pageable pageable);

    /**
     * Последние N транзакций пользователя
     */
    @Query("SELECT t FROM Transaction t WHERE t.fromCard.user.id = :userId OR t.toCard.user.id = :userId " +
           "ORDER BY t.transactionDate DESC")
    List<Transaction> findTopTransactionsByUserId(@Param("userId") Long userId, Pageable pageable);
}

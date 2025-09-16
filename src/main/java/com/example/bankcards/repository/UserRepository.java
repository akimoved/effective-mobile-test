package com.example.bankcards.repository;

import com.example.bankcards.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Репозиторий для работы с пользователями
 */
@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    /**
     * Поиск пользователя по имени пользователя
     */
    Optional<User> findByUsername(String username);

    /**
     * Поиск пользователя по email
     */
    Optional<User> findByEmail(String email);

    /**
     * Проверка существования пользователя по имени пользователя
     */
    boolean existsByUsername(String username);

    /**
     * Проверка существования пользователя по email
     */
    boolean existsByEmail(String email);

    /**
     * Поиск пользователя по имени пользователя или email
     */
    @Query("SELECT u FROM User u WHERE u.username = :login OR u.email = :login")
    Optional<User> findByUsernameOrEmail(@Param("login") String login);

    /**
     * Поиск активных пользователей
     */
    @Query("SELECT u FROM User u WHERE u.enabled = true")
    java.util.List<User> findAllActiveUsers();

    /**
     * Подсчет количества карт у пользователя
     */
    @Query("SELECT COUNT(c) FROM Card c WHERE c.user.id = :userId")
    long countCardsByUserId(@Param("userId") Long userId);
}

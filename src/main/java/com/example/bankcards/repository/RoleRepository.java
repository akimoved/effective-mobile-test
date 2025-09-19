package com.example.bankcards.repository;

import com.example.bankcards.entity.Role;
import com.example.bankcards.entity.enums.RoleName;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Репозиторий для работы с ролями
 */
@Repository
public interface RoleRepository extends JpaRepository<Role, Long> {

    /**
     * Поиск роли по названию
     */
    Optional<Role> findByName(RoleName name);

    /**
     * Проверка существования роли по названию
     */
    boolean existsByName(RoleName name);
}

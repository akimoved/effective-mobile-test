package com.example.bankcards.service;

import com.example.bankcards.entity.Role;
import com.example.bankcards.entity.RoleName;
import com.example.bankcards.entity.User;
import com.example.bankcards.exception.UserAlreadyExistsException;
import com.example.bankcards.exception.UserNotFoundException;
import com.example.bankcards.repository.RoleRepository;
import com.example.bankcards.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.Set;

/**
 * Сервис для управления пользователями
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class UserService {

    private static final String USER_WITH_ID = "Пользователь с ID ";
    private static final String NOT_FOUND = " не найден";
    private static final String USER_WITH_USERNAME = "Пользователь '";
    private static final String USER_WITH_EMAIL = "Пользователь с email '";
    private static final String ROLE_NOT_FOUND = "Роль не найдена: ";

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;

    /**
     * Создание нового пользователя
     */
    public User createUser(String username, String email, String password, RoleName roleName) {
        return createUser(username, email, password, null, null, roleName);
    }

    /**
     * Создание нового пользователя с именем и фамилией
     */
    public User createUser(String username, String email, String password, String firstName, String lastName, RoleName roleName) {
        log.info("Создание пользователя: {}", username);

        if (userRepository.existsByUsername(username)) {
            throw new UserAlreadyExistsException("Пользователь с именем '" + username + "' уже существует");
        }

        if (userRepository.existsByEmail(email)) {
            throw new UserAlreadyExistsException(USER_WITH_EMAIL + email + "' уже существует");
        }

        Role role = roleRepository.findByName(roleName)
                .orElseThrow(() -> new RuntimeException(ROLE_NOT_FOUND + roleName));

        User.UserBuilder userBuilder = User.builder()
                .username(username)
                .email(email)
                .password(passwordEncoder.encode(password))
                .enabled(true)
                .roles(Set.of(role));

        if (firstName != null) {
            userBuilder.firstName(firstName);
        }
        if (lastName != null) {
            userBuilder.lastName(lastName);
        }

        User user = userBuilder.build();
        User savedUser = userRepository.save(user);
        log.info("Пользователь создан: {}", savedUser.getUsername());
        return savedUser;
    }

    /**
     * Поиск пользователя по ID
     */
    @Transactional(readOnly = true)
    public User findById(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new UserNotFoundException(USER_WITH_ID + id + NOT_FOUND));
    }

    /**
     * Поиск пользователя по username
     */
    @Transactional(readOnly = true)
    public User findByUsername(String username) {
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new UserNotFoundException(USER_WITH_USERNAME + username + "'" + NOT_FOUND));
    }

    /**
     * Поиск пользователя по email
     */
    @Transactional(readOnly = true)
    public User findByEmail(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new UserNotFoundException(USER_WITH_EMAIL + email + "'" + NOT_FOUND));
    }

    /**
     * Поиск пользователя по username или email
     */
    @Transactional(readOnly = true)
    public Optional<User> findByUsernameOrEmail(String login) {
        return userRepository.findByUsernameOrEmail(login);
    }

    /**
     * Активация/деактивация пользователя
     */
    public User setUserEnabled(Long userId, boolean enabled) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException(USER_WITH_ID + userId + NOT_FOUND));
        user.setEnabled(enabled);
        User savedUser = userRepository.save(user);
        log.info("Пользователь {} {}", user.getUsername(), enabled ? "активирован" : "деактивирован");
        return savedUser;
    }

    /**
     * Добавление роли пользователю
     */
    public User addRoleToUser(Long userId, RoleName roleName) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException(USER_WITH_ID + userId + NOT_FOUND));
        Role role = roleRepository.findByName(roleName)
                .orElseThrow(() -> new RuntimeException(ROLE_NOT_FOUND + roleName));

        user.getRoles().add(role);
        User savedUser = userRepository.save(user);
        log.info("Роль {} добавлена пользователю {}", roleName, user.getUsername());
        return savedUser;
    }

    /**
     * Удаление роли у пользователя
     */
    public User removeRoleFromUser(Long userId, RoleName roleName) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException(USER_WITH_ID + userId + NOT_FOUND));
        Role role = roleRepository.findByName(roleName)
                .orElseThrow(() -> new RuntimeException(ROLE_NOT_FOUND + roleName));

        user.getRoles().remove(role);
        User savedUser = userRepository.save(user);
        log.info("Роль {} удалена у пользователя {}", roleName, user.getUsername());
        return savedUser;
    }

    /**
     * Проверка наличия роли у пользователя
     */
    @Transactional(readOnly = true)
    public boolean hasRole(Long userId, RoleName roleName) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException(USER_WITH_ID + userId + NOT_FOUND));
        return user.getRoles().stream()
                .anyMatch(role -> role.getName() == roleName);
    }

    /**
     * Смена пароля
     */
    public void changePassword(Long userId, String newPassword) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException(USER_WITH_ID + userId + NOT_FOUND));
        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);
        log.info("Пароль изменен для пользователя: {}", user.getUsername());
    }

    /**
     * Получение количества карт пользователя
     */
    @Transactional(readOnly = true)
    public long getUserCardCount(Long userId) {
        return userRepository.countCardsByUserId(userId);
    }
}

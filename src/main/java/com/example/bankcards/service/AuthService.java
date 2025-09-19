package com.example.bankcards.service;

import com.example.bankcards.dto.response.AuthResponse;
import com.example.bankcards.entity.enums.RoleName;
import com.example.bankcards.entity.User;
import io.jsonwebtoken.JwtException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Сервис аутентификации
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class AuthService {

    private final UserService userService;
    private final JwtService jwtService;
    private final CustomUserDetailsService userDetailsService;
    private final AuthenticationManager authenticationManager;

    /**
     * Регистрация нового пользователя
     */
    public AuthResponse register(String username, String email, String password, String firstName, String lastName) {
        log.info("Регистрация пользователя: {}", username);

        User createdUser = userService.createUser(username, email, password, firstName, lastName, RoleName.ROLE_USER);
        UserDetails userDetails = userDetailsService.loadUserByUsername(createdUser.getUsername());
        String token = jwtService.generateToken(userDetails);

        log.info("Пользователь {} успешно зарегистрирован", username);
        return new AuthResponse(token, jwtService.getExpirationTime());
    }

    /**
     * Аутентификация пользователя
     */
    public AuthResponse authenticate(String login, String password) {
        log.info("Попытка входа пользователя: {}", login);

        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(login, password)
        );

        log.info("Call UDS");
        UserDetails userDetails = (UserDetails) authentication.getPrincipal();
        String token = jwtService.generateToken(userDetails);

        log.info("Пользователь {} успешно аутентифицирован", login);
        return new AuthResponse(token, jwtService.getExpirationTime());
    }

    /**
     * Проверка валидности токена
     */
    public boolean validateToken(String token, String username) {
        try {
            UserDetails userDetails = userDetailsService.loadUserByUsername(username);
            return jwtService.isTokenValid(token, userDetails);
        } catch (Exception e) {
            log.warn("Ошибка валидации токена для пользователя {}: {}", username, e.getMessage());
            return false;
        }
    }

    /**
     * Извлечение username из токена
     */
    public String extractUsername(String token) {
        try {
            return jwtService.extractUsername(token);
        } catch (Exception e) {
            log.warn("Ошибка извлечения username из токена: {}", e.getMessage());
            return null;
        }
    }
}

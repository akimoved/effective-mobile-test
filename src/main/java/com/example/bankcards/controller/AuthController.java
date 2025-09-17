package com.example.bankcards.controller;

import com.example.bankcards.dto.response.AuthResponse;
import com.example.bankcards.dto.request.LoginRequest;
import com.example.bankcards.dto.request.RegisterRequest;
import com.example.bankcards.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Контроллер аутентификации
 */
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Slf4j
public class AuthController {

    private final AuthService authService;

    /**
     * Регистрация нового пользователя
     */
    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(@Valid @RequestBody RegisterRequest request) {
        log.info("Запрос на регистрацию пользователя: {}", request.username());
        
        AuthResponse response = authService.register(
                request.username(),
                request.email(),
                request.password(),
                request.firstName(),
                request.lastName()
        );
        
        return ResponseEntity.ok(response);
    }

    /**
     * Аутентификация пользователя
     */
    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        log.info("Запрос на аутентификацию пользователя: {}", request.login());
        
        AuthResponse response = authService.authenticate(request.login(), request.password());
        
        return ResponseEntity.ok(response);
    }

    /**
     * Проверка валидности токена
     */
    @PostMapping("/validate")
    public ResponseEntity<Boolean> validateToken(@RequestHeader("Authorization") String authHeader) {
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return ResponseEntity.ok(false);
        }
        
        String token = authHeader.substring(7);
        String username = authService.extractUsername(token);
        boolean isValid = authService.validateToken(token, username);
        
        return ResponseEntity.ok(isValid);
    }
}

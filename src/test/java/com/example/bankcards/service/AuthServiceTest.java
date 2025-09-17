package com.example.bankcards.service;

import com.example.bankcards.dto.response.AuthResponse;
import com.example.bankcards.entity.RoleName;
import com.example.bankcards.entity.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserService userService;

    @Mock
    private JwtService jwtService;

    @Mock
    private CustomUserDetailsService userDetailsService;

    @Mock
    private AuthenticationManager authenticationManager;

    @Mock
    private UserDetails userDetails;

    @InjectMocks
    private AuthService authService;

    private User testUser;

    @BeforeEach
    void setUp() {
        testUser = User.builder()
                .id(1L)
                .username("testuser")
                .email("test@example.com")
                .firstName("Test")
                .lastName("User")
                .enabled(true)
                .build();
    }

    @Test
    void register_ShouldCreateUserAndReturnToken() {
        String username = "newuser";
        String email = "new@example.com";
        String password = "password123";
        String firstName = "New";
        String lastName = "User";
        String expectedToken = "jwt-token";
        long expectedExpiration = 86400000L;

        when(userService.createUser(username, email, password, firstName, lastName, RoleName.USER)).thenReturn(testUser);
        when(userDetailsService.loadUserByUsername(testUser.getUsername())).thenReturn(userDetails);
        when(jwtService.generateToken(userDetails)).thenReturn(expectedToken);
        when(jwtService.getExpirationTime()).thenReturn(expectedExpiration);

        AuthResponse result = authService.register(username, email, password, firstName, lastName);

        assertNotNull(result);
        assertEquals(expectedToken, result.token());
        assertEquals(expectedExpiration, result.expiresIn());

        verify(userService).createUser(username, email, password, firstName, lastName, RoleName.USER);
        verify(userDetailsService).loadUserByUsername(testUser.getUsername());
        verify(jwtService).generateToken(userDetails);
    }

    @Test
    void authenticate_ShouldAuthenticateAndReturnToken() {
        String login = "testuser";
        String password = "password123";
        String expectedToken = "jwt-token";
        long expectedExpiration = 86400000L;

        when(userDetailsService.loadUserByUsername(login)).thenReturn(userDetails);
        when(jwtService.generateToken(userDetails)).thenReturn(expectedToken);
        when(jwtService.getExpirationTime()).thenReturn(expectedExpiration);

        AuthResponse result = authService.authenticate(login, password);

        assertNotNull(result);
        assertEquals(expectedToken, result.token());
        assertEquals(expectedExpiration, result.expiresIn());

        verify(authenticationManager).authenticate(any(UsernamePasswordAuthenticationToken.class));
        verify(userDetailsService).loadUserByUsername(login);
        verify(jwtService).generateToken(userDetails);
    }

    @Test
    void validateToken_ShouldReturnTrue_WhenTokenIsValid() {
        String token = "valid-token";
        String username = "testuser";

        when(userDetailsService.loadUserByUsername(username)).thenReturn(userDetails);
        when(jwtService.isTokenValid(token, userDetails)).thenReturn(true);

        boolean result = authService.validateToken(token, username);

        assertTrue(result);
        verify(userDetailsService).loadUserByUsername(username);
        verify(jwtService).isTokenValid(token, userDetails);
    }

    @Test
    void validateToken_ShouldReturnFalse_WhenTokenIsInvalid() {
        String token = "invalid-token";
        String username = "testuser";

        when(userDetailsService.loadUserByUsername(username)).thenReturn(userDetails);
        when(jwtService.isTokenValid(token, userDetails)).thenReturn(false);

        boolean result = authService.validateToken(token, username);

        assertFalse(result);
    }

    @Test
    void validateToken_ShouldReturnFalse_WhenExceptionOccurs() {
        String token = "token";
        String username = "testuser";

        when(userDetailsService.loadUserByUsername(username)).thenThrow(new RuntimeException("User not found"));

        boolean result = authService.validateToken(token, username);

        assertFalse(result);
    }

    @Test
    void extractUsername_ShouldReturnUsername() {
        String token = "jwt-token";
        String expectedUsername = "testuser";

        when(jwtService.extractUsername(token)).thenReturn(expectedUsername);

        String result = authService.extractUsername(token);

        assertEquals(expectedUsername, result);
        verify(jwtService).extractUsername(token);
    }
}

package com.example.bankcards.service;

import com.example.bankcards.entity.Role;
import com.example.bankcards.entity.RoleName;
import com.example.bankcards.entity.User;
import com.example.bankcards.exception.UserAlreadyExistsException;
import com.example.bankcards.exception.UserNotFoundException;
import com.example.bankcards.repository.RoleRepository;
import com.example.bankcards.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private RoleRepository roleRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private UserService userService;

    private User testUser;
    private Role userRole;

    @BeforeEach
    void setUp() {
        userRole = Role.builder()
                .id(1L)
                .name(RoleName.USER)
                .build();

        testUser = User.builder()
                .id(1L)
                .username("testuser")
                .email("test@example.com")
                .password("encodedPassword")
                .enabled(true)
                .roles(new HashSet<>(Set.of(userRole)))
                .build();
    }

    @Test
    void createUser_ShouldCreateUserSuccessfully() {
        String username = "newuser";
        String email = "new@example.com";
        String password = "password123";
        String encodedPassword = "encodedPassword123";

        when(userRepository.existsByUsername(username)).thenReturn(false);
        when(userRepository.existsByEmail(email)).thenReturn(false);
        when(roleRepository.findByName(RoleName.USER)).thenReturn(Optional.of(userRole));
        when(passwordEncoder.encode(password)).thenReturn(encodedPassword);
        when(userRepository.save(any(User.class))).thenReturn(testUser);

        User result = userService.createUser(username, email, password, RoleName.USER);

        assertNotNull(result);
        verify(userRepository).existsByUsername(username);
        verify(userRepository).existsByEmail(email);
        verify(passwordEncoder).encode(password);
        verify(userRepository).save(any(User.class));
    }

    @Test
    void createUser_WithExistingUsername_ShouldThrowException() {
        String username = "existinguser";
        when(userRepository.existsByUsername(username)).thenReturn(true);

        assertThrows(UserAlreadyExistsException.class, () ->
                userService.createUser(username, "email@test.com", "password", RoleName.USER));
    }

    @Test
    void createUser_WithExistingEmail_ShouldThrowException() {
        String email = "existing@example.com";
        when(userRepository.existsByUsername(anyString())).thenReturn(false);
        when(userRepository.existsByEmail(email)).thenReturn(true);

        assertThrows(UserAlreadyExistsException.class, () ->
                userService.createUser("username", email, "password", RoleName.USER));
    }

    @Test
    void findById_ShouldReturnUser() {
        Long userId = 1L;
        when(userRepository.findById(userId)).thenReturn(Optional.of(testUser));

        User result = userService.findById(userId);

        assertEquals(testUser, result);
        verify(userRepository).findById(userId);
    }

    @Test
    void findById_WithNonExistentId_ShouldThrowException() {
        Long userId = 999L;
        when(userRepository.findById(userId)).thenReturn(Optional.empty());

        assertThrows(UserNotFoundException.class, () -> userService.findById(userId));
    }

    @Test
    void findByUsername_ShouldReturnUser() {
        String username = "testuser";
        when(userRepository.findByUsername(username)).thenReturn(Optional.of(testUser));

        User result = userService.findByUsername(username);

        assertEquals(testUser, result);
        verify(userRepository).findByUsername(username);
    }

    @Test
    void findByUsername_WithNonExistentUsername_ShouldThrowException() {
        String username = "nonexistent";
        when(userRepository.findByUsername(username)).thenReturn(Optional.empty());

        assertThrows(UserNotFoundException.class, () -> userService.findByUsername(username));
    }

    @Test
    void setUserEnabled_ShouldUpdateUserStatus() {
        Long userId = 1L;
        when(userRepository.findById(userId)).thenReturn(Optional.of(testUser));
        when(userRepository.save(testUser)).thenReturn(testUser);

        User result = userService.setUserEnabled(userId, false);

        assertFalse(result.getEnabled());
        verify(userRepository).save(testUser);
    }

    @Test
    void addRoleToUser_ShouldAddRole() {
        Long userId = 1L;
        Role adminRole = Role.builder().id(2L).name(RoleName.ADMIN).build();
        
        when(userRepository.findById(userId)).thenReturn(Optional.of(testUser));
        when(roleRepository.findByName(RoleName.ADMIN)).thenReturn(Optional.of(adminRole));
        when(userRepository.save(testUser)).thenReturn(testUser);

        User result = userService.addRoleToUser(userId, RoleName.ADMIN);

        assertTrue(result.getRoles().contains(adminRole));
        verify(userRepository).save(testUser);
    }

    @Test
    void hasRole_ShouldReturnTrue_WhenUserHasRole() {
        Long userId = 1L;
        when(userRepository.findById(userId)).thenReturn(Optional.of(testUser));

        boolean result = userService.hasRole(userId, RoleName.USER);

        assertTrue(result);
    }

    @Test
    void hasRole_ShouldReturnFalse_WhenUserDoesNotHaveRole() {
        Long userId = 1L;
        when(userRepository.findById(userId)).thenReturn(Optional.of(testUser));

        boolean result = userService.hasRole(userId, RoleName.ADMIN);

        assertFalse(result);
    }

    @Test
    void changePassword_ShouldUpdatePassword() {
        Long userId = 1L;
        String newPassword = "newPassword123";
        String encodedNewPassword = "encodedNewPassword123";
        
        when(userRepository.findById(userId)).thenReturn(Optional.of(testUser));
        when(passwordEncoder.encode(newPassword)).thenReturn(encodedNewPassword);

        userService.changePassword(userId, newPassword);

        assertEquals(encodedNewPassword, testUser.getPassword());
        verify(passwordEncoder).encode(newPassword);
        verify(userRepository).save(testUser);
    }

    @Test
    void getUserCardCount_ShouldReturnCount() {
        Long userId = 1L;
        long expectedCount = 3L;
        when(userRepository.countCardsByUserId(userId)).thenReturn(expectedCount);

        long result = userService.getUserCardCount(userId);

        assertEquals(expectedCount, result);
        verify(userRepository).countCardsByUserId(userId);
    }
}

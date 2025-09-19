package com.example.bankcards.service;

import com.example.bankcards.dto.request.CardCreateRequest;
import com.example.bankcards.dto.response.CardResponse;
import com.example.bankcards.dto.request.CardUpdateRequest;
import com.example.bankcards.entity.Card;
import com.example.bankcards.entity.enums.CardStatus;
import com.example.bankcards.entity.enums.RoleName;
import com.example.bankcards.entity.User;
import com.example.bankcards.exception.CardNotFoundException;
import com.example.bankcards.exception.DuplicateCardNumberException;
import com.example.bankcards.repository.CardRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CardServiceTest {

    @Mock
    private CardRepository cardRepository;

    @Mock
    private UserService userService;

    @Mock
    private EncryptionService encryptionService;

    @InjectMocks
    private CardService cardService;

    private User testUser;
    private Card testCard;
    private CardCreateRequest createRequest;
    private CardUpdateRequest updateRequest;

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

        testCard = Card.builder()
                .id(1L)
                .cardNumber("1234567890123456")
                .cardHolderName("Test User")
                .expiryDate(LocalDate.now().plusYears(3))
                .status(CardStatus.ACTIVE)
                .user(testUser)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        createRequest = new CardCreateRequest("1234567890123456", "Test User");
        updateRequest = new CardUpdateRequest("Updated Name", CardStatus.BLOCKED);
    }

    @Test
    void createCard_ShouldCreateCardSuccessfully() {
        String encryptedCardNumber = "encrypted123456";
        String maskedCardNumber = "**** **** **** 3456";
        
        when(userService.findByUsername("testuser")).thenReturn(testUser);
        when(encryptionService.encrypt("1234567890123456")).thenReturn(encryptedCardNumber);
        when(cardRepository.existsByCardNumber(encryptedCardNumber)).thenReturn(false);
        when(cardRepository.save(any(Card.class))).thenReturn(testCard);
        when(encryptionService.decrypt(testCard.getCardNumber())).thenReturn("1234567890123456");
        when(encryptionService.maskCardNumber("1234567890123456")).thenReturn(maskedCardNumber);

        CardResponse result = cardService.createCard(createRequest, "testuser");

        assertNotNull(result);
        assertEquals(testCard.getId(), result.id());
        assertEquals(maskedCardNumber, result.cardNumber());
        assertEquals(testCard.getCardHolderName(), result.cardholderName());
        assertEquals(testCard.getStatus(), result.status());

        verify(userService).findByUsername("testuser");
        verify(encryptionService).encrypt("1234567890123456");
        verify(cardRepository).existsByCardNumber(encryptedCardNumber);
        verify(cardRepository).save(any(Card.class));
    }

    @Test
    void createCard_ShouldThrowException_WhenCardNumberExists() {
        String encryptedCardNumber = "encrypted123456";
        String maskedCardNumber = "**** **** **** 3456";
        
        when(userService.findByUsername("testuser")).thenReturn(testUser);
        when(encryptionService.encrypt("1234567890123456")).thenReturn(encryptedCardNumber);
        when(cardRepository.existsByCardNumber(encryptedCardNumber)).thenReturn(true);
        when(encryptionService.maskCardNumber("1234567890123456")).thenReturn(maskedCardNumber);

        assertThrows(DuplicateCardNumberException.class, 
                () -> cardService.createCard(createRequest, "testuser"));

        verify(cardRepository, never()).save(any(Card.class));
    }

    @Test
    void getCard_ShouldReturnCard_WhenUserOwnsCard() {
        String maskedCardNumber = "**** **** **** 3456";
        
        when(cardRepository.findById(1L)).thenReturn(Optional.of(testCard));
        when(encryptionService.decrypt(testCard.getCardNumber())).thenReturn("1234567890123456");
        when(encryptionService.maskCardNumber("1234567890123456")).thenReturn(maskedCardNumber);

        CardResponse result = cardService.getCard(1L, "testuser");

        assertNotNull(result);
        assertEquals(testCard.getId(), result.id());
        assertEquals(maskedCardNumber, result.cardNumber());
        verify(cardRepository).findById(1L);
    }

    @Test
    void getCard_ShouldThrowException_WhenCardNotFound() {
        when(cardRepository.findById(1L)).thenReturn(Optional.empty());

        assertThrows(CardNotFoundException.class, 
                () -> cardService.getCard(1L, "testuser"));
    }

    @Test
    void getCard_ShouldThrowException_WhenUserDoesNotOwnCard() {
        when(cardRepository.findById(1L)).thenReturn(Optional.of(testCard));
        when(userService.findByUsername("otheruser")).thenReturn(
                User.builder().id(2L).username("otheruser").build());
        when(userService.hasRole(2L, RoleName.ROLE_ADMIN)).thenReturn(false);

        assertThrows(AccessDeniedException.class, 
                () -> cardService.getCard(1L, "otheruser"));
    }

    @Test
    void getUserCards_ShouldReturnUserCards() {
        Pageable pageable = PageRequest.of(0, 10);
        Page<Card> cardPage = new PageImpl<>(List.of(testCard));
        
        when(userService.findByUsername("testuser")).thenReturn(testUser);
        when(cardRepository.findByUserIdOrderByCreatedAtDesc(1L, pageable)).thenReturn(cardPage);

        Page<CardResponse> result = cardService.getUserCards("testuser", pageable);

        assertNotNull(result);
        assertEquals(1, result.getTotalElements());
        assertEquals(testCard.getId(), result.getContent().get(0).id());

        verify(userService).findByUsername("testuser");
        verify(cardRepository).findByUserIdOrderByCreatedAtDesc(1L, pageable);
    }

    @Test
    void updateCard_ShouldUpdateCardSuccessfully() {
        when(cardRepository.findById(1L)).thenReturn(Optional.of(testCard));
        when(cardRepository.save(any(Card.class))).thenReturn(testCard);

        CardResponse result = cardService.updateCard(1L, updateRequest, "testuser");

        assertNotNull(result);
        verify(cardRepository).findById(1L);
        verify(cardRepository).save(testCard);
    }

    @Test
    void blockCard_ShouldBlockCardSuccessfully() {
        when(cardRepository.findById(1L)).thenReturn(Optional.of(testCard));
        when(cardRepository.save(any(Card.class))).thenReturn(testCard);

        CardResponse result = cardService.blockCard(1L, "testuser");

        assertNotNull(result);
        assertEquals(CardStatus.BLOCKED, testCard.getStatus());
        verify(cardRepository).save(testCard);
    }

    @Test
    void unblockCard_ShouldUnblockCardSuccessfully() {
        testCard.setStatus(CardStatus.BLOCKED);
        when(cardRepository.findById(1L)).thenReturn(Optional.of(testCard));
        when(cardRepository.save(any(Card.class))).thenReturn(testCard);

        CardResponse result = cardService.unblockCard(1L, "testuser");

        assertNotNull(result);
        assertEquals(CardStatus.ACTIVE, testCard.getStatus());
        verify(cardRepository).save(testCard);
    }

    @Test
    void deleteCard_ShouldDeleteCardSuccessfully() {
        when(cardRepository.findById(1L)).thenReturn(Optional.of(testCard));

        cardService.deleteCard(1L, "testuser");

        verify(cardRepository).findById(1L);
        verify(cardRepository).delete(testCard);
    }

    @Test
    void findByCardNumber_ShouldReturnCard_WhenUserIsAdmin() {
        User adminUser = User.builder().id(2L).username("admin").build();
        String encryptedCardNumber = "encrypted123456";
        String maskedCardNumber = "**** **** **** 3456";
        
        when(userService.findByUsername("admin")).thenReturn(adminUser);
        when(userService.hasRole(2L, RoleName.ROLE_ADMIN)).thenReturn(true);
        when(encryptionService.encrypt("1234567890123456")).thenReturn(encryptedCardNumber);
        when(cardRepository.findByCardNumber(encryptedCardNumber)).thenReturn(Optional.of(testCard));
        when(encryptionService.decrypt(testCard.getCardNumber())).thenReturn("1234567890123456");
        when(encryptionService.maskCardNumber("1234567890123456")).thenReturn(maskedCardNumber);

        CardResponse result = cardService.findByCardNumber("1234567890123456", "admin");

        assertNotNull(result);
        assertEquals(testCard.getId(), result.id());
        assertEquals(maskedCardNumber, result.cardNumber());
        verify(userService).hasRole(2L, RoleName.ROLE_ADMIN);
        verify(encryptionService).encrypt("1234567890123456");
        verify(cardRepository).findByCardNumber(encryptedCardNumber);
    }

    @Test
    void findByCardNumber_ShouldThrowException_WhenUserIsNotAdmin() {
        User regularUser = User.builder().id(2L).username("user").build();
        when(userService.findByUsername("user")).thenReturn(regularUser);
        when(userService.hasRole(2L, RoleName.ROLE_ADMIN)).thenReturn(false);

        assertThrows(AccessDeniedException.class, 
                () -> cardService.findByCardNumber("1234567890123456", "user"));

        verify(cardRepository, never()).findByCardNumber(anyString());
    }

    @Test
    void getAllCards_ShouldReturnAllCards_WhenUserIsAdmin() {
        User adminUser = User.builder().id(2L).username("admin").build();
        Pageable pageable = PageRequest.of(0, 20);
        Page<Card> cardPage = new PageImpl<>(List.of(testCard));

        when(userService.findByUsername("admin")).thenReturn(adminUser);
        when(userService.hasRole(2L, RoleName.ROLE_ADMIN)).thenReturn(true);
        when(cardRepository.findAllByOrderByCreatedAtDesc(pageable)).thenReturn(cardPage);

        Page<CardResponse> result = cardService.getAllCards("admin", pageable);

        assertNotNull(result);
        assertEquals(1, result.getTotalElements());
        verify(userService).hasRole(2L, RoleName.ROLE_ADMIN);
        verify(cardRepository).findAllByOrderByCreatedAtDesc(pageable);
    }

    @Test
    void getAllCards_ShouldThrowException_WhenUserIsNotAdmin() {
        User regularUser = User.builder().id(2L).username("user").build();
        Pageable pageable = PageRequest.of(0, 20);

        when(userService.findByUsername("user")).thenReturn(regularUser);
        when(userService.hasRole(2L, RoleName.ROLE_ADMIN)).thenReturn(false);

        assertThrows(AccessDeniedException.class, 
                () -> cardService.getAllCards("user", pageable));

        verify(cardRepository, never()).findAllByOrderByCreatedAtDesc(any(Pageable.class));
    }
}

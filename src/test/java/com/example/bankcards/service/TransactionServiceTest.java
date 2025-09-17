package com.example.bankcards.service;

import com.example.bankcards.dto.response.BalanceResponse;
import com.example.bankcards.dto.request.TransactionCreateRequest;
import com.example.bankcards.dto.response.TransactionResponse;
import com.example.bankcards.entity.Card;
import com.example.bankcards.entity.CardStatus;
import com.example.bankcards.entity.RoleName;
import com.example.bankcards.entity.Transaction;
import com.example.bankcards.entity.TransactionStatus;
import com.example.bankcards.entity.User;
import com.example.bankcards.exception.CardNotFoundException;
import com.example.bankcards.exception.InsufficientFundsException;
import com.example.bankcards.exception.InvalidTransactionException;
import com.example.bankcards.exception.TransactionNotFoundException;
import com.example.bankcards.repository.CardRepository;
import com.example.bankcards.repository.TransactionRepository;
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

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TransactionServiceTest {

    @Mock
    private TransactionRepository transactionRepository;

    @Mock
    private CardRepository cardRepository;

    @Mock
    private UserService userService;

    @Mock
    private EncryptionService encryptionService;

    @InjectMocks
    private TransactionService transactionService;

    private User testUser;
    private Card fromCard;
    private Card toCard;
    private Transaction testTransaction;
    private TransactionCreateRequest createRequest;

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

        fromCard = Card.builder()
                .id(1L)
                .cardNumber("1234567890123456")
                .cardHolderName("Test User")
                .expiryDate(LocalDate.now().plusYears(3))
                .status(CardStatus.ACTIVE)
                .balance(new BigDecimal("1000.00"))
                .user(testUser)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        toCard = Card.builder()
                .id(2L)
                .cardNumber("9876543210987654")
                .cardHolderName("Test User")
                .expiryDate(LocalDate.now().plusYears(3))
                .status(CardStatus.ACTIVE)
                .balance(new BigDecimal("500.00"))
                .user(testUser)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        testTransaction = Transaction.builder()
                .id(1L)
                .fromCard(fromCard)
                .toCard(toCard)
                .amount(new BigDecimal("100.00"))
                .description("Test transfer")
                .status(TransactionStatus.COMPLETED)
                .transactionDate(LocalDateTime.now())
                .completedAt(LocalDateTime.now())
                .build();

        createRequest = new TransactionCreateRequest(1L, 2L, new BigDecimal("100.00"), "Test transfer");
    }

    @Test
    void createTransfer_ShouldCreateTransferSuccessfully() {
        when(userService.findByUsername("testuser")).thenReturn(testUser);
        when(cardRepository.findById(1L)).thenReturn(Optional.of(fromCard));
        when(cardRepository.findById(2L)).thenReturn(Optional.of(toCard));
        when(transactionRepository.save(any(Transaction.class))).thenReturn(testTransaction);

        TransactionResponse result = transactionService.createTransfer(createRequest, "testuser");

        assertNotNull(result);
        assertEquals(testTransaction.getId(), result.id());
        assertEquals(testTransaction.getAmount(), result.amount());
        assertEquals(TransactionStatus.COMPLETED, result.status());

        verify(userService).findByUsername("testuser");
        verify(cardRepository).findById(1L);
        verify(cardRepository).findById(2L);
        verify(transactionRepository, times(2)).save(any(Transaction.class));
        verify(cardRepository, times(2)).save(any(Card.class));
    }

    @Test
    void createTransfer_ShouldThrowException_WhenFromCardNotFound() {
        when(userService.findByUsername("testuser")).thenReturn(testUser);
        when(cardRepository.findById(1L)).thenReturn(Optional.empty());

        assertThrows(CardNotFoundException.class, 
                () -> transactionService.createTransfer(createRequest, "testuser"));

        verify(transactionRepository, never()).save(any(Transaction.class));
    }

    @Test
    void createTransfer_ShouldThrowException_WhenToCardNotFound() {
        when(userService.findByUsername("testuser")).thenReturn(testUser);
        when(cardRepository.findById(1L)).thenReturn(Optional.of(fromCard));
        when(cardRepository.findById(2L)).thenReturn(Optional.empty());

        assertThrows(CardNotFoundException.class, 
                () -> transactionService.createTransfer(createRequest, "testuser"));

        verify(transactionRepository, never()).save(any(Transaction.class));
    }

    @Test
    void createTransfer_ShouldThrowException_WhenSameCard() {
        TransactionCreateRequest sameCardRequest = new TransactionCreateRequest(1L, 1L, new BigDecimal("100.00"), "Test");
        
        when(userService.findByUsername("testuser")).thenReturn(testUser);
        when(cardRepository.findById(1L)).thenReturn(Optional.of(fromCard));

        assertThrows(InvalidTransactionException.class, 
                () -> transactionService.createTransfer(sameCardRequest, "testuser"));
    }

    @Test
    void createTransfer_ShouldThrowException_WhenInsufficientFunds() {
        TransactionCreateRequest largeAmountRequest = new TransactionCreateRequest(1L, 2L, new BigDecimal("2000.00"), "Test");
        
        when(userService.findByUsername("testuser")).thenReturn(testUser);
        when(cardRepository.findById(1L)).thenReturn(Optional.of(fromCard));
        when(cardRepository.findById(2L)).thenReturn(Optional.of(toCard));

        assertThrows(InsufficientFundsException.class, 
                () -> transactionService.createTransfer(largeAmountRequest, "testuser"));
    }

    @Test
    void createTransfer_ShouldThrowException_WhenFromCardNotActive() {
        fromCard.setStatus(CardStatus.BLOCKED);
        
        when(userService.findByUsername("testuser")).thenReturn(testUser);
        when(cardRepository.findById(1L)).thenReturn(Optional.of(fromCard));
        when(cardRepository.findById(2L)).thenReturn(Optional.of(toCard));

        assertThrows(InvalidTransactionException.class, 
                () -> transactionService.createTransfer(createRequest, "testuser"));
    }

    @Test
    void createTransfer_ShouldThrowException_WhenToCardNotActive() {
        toCard.setStatus(CardStatus.BLOCKED);
        
        when(userService.findByUsername("testuser")).thenReturn(testUser);
        when(cardRepository.findById(1L)).thenReturn(Optional.of(fromCard));
        when(cardRepository.findById(2L)).thenReturn(Optional.of(toCard));

        assertThrows(InvalidTransactionException.class, 
                () -> transactionService.createTransfer(createRequest, "testuser"));
    }

    @Test
    void createTransfer_ShouldThrowException_WhenCardsNotOwnedByUser() {
        User otherUser = User.builder().id(2L).username("otheruser").build();
        toCard.setUser(otherUser);
        
        when(userService.findByUsername("testuser")).thenReturn(testUser);
        when(cardRepository.findById(1L)).thenReturn(Optional.of(fromCard));
        when(cardRepository.findById(2L)).thenReturn(Optional.of(toCard));

        assertThrows(InvalidTransactionException.class, 
                () -> transactionService.createTransfer(createRequest, "testuser"));
    }

    @Test
    void getTransaction_ShouldReturnTransaction_WhenUserOwnsTransaction() {
        when(transactionRepository.findById(1L)).thenReturn(Optional.of(testTransaction));
        when(userService.findByUsername("testuser")).thenReturn(testUser);

        TransactionResponse result = transactionService.getTransaction(1L, "testuser");

        assertNotNull(result);
        assertEquals(testTransaction.getId(), result.id());
        verify(transactionRepository).findById(1L);
    }

    @Test
    void getTransaction_ShouldThrowException_WhenTransactionNotFound() {
        when(transactionRepository.findById(1L)).thenReturn(Optional.empty());

        assertThrows(TransactionNotFoundException.class, 
                () -> transactionService.getTransaction(1L, "testuser"));
    }

    @Test
    void getTransaction_ShouldThrowException_WhenUserDoesNotOwnTransaction() {
        User otherUser = User.builder().id(2L).username("otheruser").build();
        fromCard.setUser(otherUser);
        toCard.setUser(otherUser);
        
        when(transactionRepository.findById(1L)).thenReturn(Optional.of(testTransaction));
        when(userService.findByUsername("testuser")).thenReturn(testUser);
        when(userService.hasRole(1L, RoleName.ADMIN)).thenReturn(false);

        assertThrows(AccessDeniedException.class, 
                () -> transactionService.getTransaction(1L, "testuser"));
    }

    @Test
    void getUserTransactions_ShouldReturnUserTransactions() {
        Pageable pageable = PageRequest.of(0, 10);
        Page<Transaction> transactionPage = new PageImpl<>(List.of(testTransaction));
        
        when(userService.findByUsername("testuser")).thenReturn(testUser);
        when(transactionRepository.findByUserId(1L, pageable)).thenReturn(transactionPage);

        Page<TransactionResponse> result = transactionService.getUserTransactions("testuser", pageable);

        assertNotNull(result);
        assertEquals(1, result.getTotalElements());
        assertEquals(testTransaction.getId(), result.getContent().get(0).id());

        verify(userService).findByUsername("testuser");
        verify(transactionRepository).findByUserId(1L, pageable);
    }

    @Test
    void getCardTransactions_ShouldReturnCardTransactions() {
        Pageable pageable = PageRequest.of(0, 10);
        Page<Transaction> transactionPage = new PageImpl<>(List.of(testTransaction));
        
        when(cardRepository.findById(1L)).thenReturn(Optional.of(fromCard));
        when(transactionRepository.findByCardId(1L, pageable)).thenReturn(transactionPage);

        Page<TransactionResponse> result = transactionService.getCardTransactions(1L, "testuser", pageable);

        assertNotNull(result);
        assertEquals(1, result.getTotalElements());
        verify(cardRepository).findById(1L);
        verify(transactionRepository).findByCardId(1L, pageable);
    }

    @Test
    void getInternalTransfers_ShouldReturnInternalTransfers() {
        Pageable pageable = PageRequest.of(0, 10);
        Page<Transaction> transactionPage = new PageImpl<>(List.of(testTransaction));
        
        when(userService.findByUsername("testuser")).thenReturn(testUser);
        when(transactionRepository.findInternalTransfersByUserId(1L, pageable)).thenReturn(transactionPage);

        Page<TransactionResponse> result = transactionService.getInternalTransfers("testuser", pageable);

        assertNotNull(result);
        assertEquals(1, result.getTotalElements());
        verify(userService).findByUsername("testuser");
        verify(transactionRepository).findInternalTransfersByUserId(1L, pageable);
    }

    @Test
    void getCardBalance_ShouldReturnBalance() {
        String maskedCardNumber = "**** **** **** 3456";
        
        when(cardRepository.findById(1L)).thenReturn(Optional.of(fromCard));
        when(encryptionService.decrypt(fromCard.getCardNumber())).thenReturn("1234567890123456");
        when(encryptionService.maskCardNumber("1234567890123456")).thenReturn(maskedCardNumber);

        BalanceResponse result = transactionService.getCardBalance(1L, "testuser");

        assertNotNull(result);
        assertEquals(fromCard.getId(), result.cardId());
        assertEquals(fromCard.getBalance(), result.balance());
        assertEquals(maskedCardNumber, result.maskedCardNumber());
        assertEquals(fromCard.getCardHolderName(), result.cardholderName());

        verify(cardRepository).findById(1L);
        verify(encryptionService).decrypt(fromCard.getCardNumber());
        verify(encryptionService).maskCardNumber("1234567890123456");
    }

    @Test
    void getUserBalances_ShouldReturnUserBalances() {
        Pageable pageable = PageRequest.of(0, 10);
        Page<Card> cardPage = new PageImpl<>(List.of(fromCard, toCard));
        
        when(userService.findByUsername("testuser")).thenReturn(testUser);
        when(cardRepository.findByUserIdOrderByCreatedAtDesc(1L, pageable)).thenReturn(cardPage);

        Page<BalanceResponse> result = transactionService.getUserBalances("testuser", pageable);

        assertNotNull(result);
        assertEquals(2, result.getTotalElements());
        verify(userService).findByUsername("testuser");
        verify(cardRepository).findByUserIdOrderByCreatedAtDesc(1L, pageable);
    }

    @Test
    void getAllTransactions_ShouldReturnAllTransactions_WhenUserIsAdmin() {
        User adminUser = User.builder().id(2L).username("admin").build();
        Pageable pageable = PageRequest.of(0, 20);
        Page<Transaction> transactionPage = new PageImpl<>(List.of(testTransaction));

        when(userService.findByUsername("admin")).thenReturn(adminUser);
        when(userService.hasRole(2L, RoleName.ADMIN)).thenReturn(true);
        when(transactionRepository.findAll(pageable)).thenReturn(transactionPage);

        Page<TransactionResponse> result = transactionService.getAllTransactions("admin", pageable);

        assertNotNull(result);
        assertEquals(1, result.getTotalElements());
        verify(userService).hasRole(2L, RoleName.ADMIN);
        verify(transactionRepository).findAll(pageable);
    }

    @Test
    void getAllTransactions_ShouldThrowException_WhenUserIsNotAdmin() {
        User regularUser = User.builder().id(2L).username("user").build();
        Pageable pageable = PageRequest.of(0, 20);

        when(userService.findByUsername("user")).thenReturn(regularUser);
        when(userService.hasRole(2L, RoleName.ADMIN)).thenReturn(false);

        assertThrows(AccessDeniedException.class, 
                () -> transactionService.getAllTransactions("user", pageable));

        verify(transactionRepository, never()).findAll(any(Pageable.class));
    }
}

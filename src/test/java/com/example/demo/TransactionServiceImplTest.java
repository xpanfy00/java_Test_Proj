package com.example.demo;

import com.example.demo.api.PostTransactionRequest;
import com.example.demo.domain.BankAccount;
import com.example.demo.exception.InternalTransactionException;
import com.example.demo.repository.BankAccountRepository;
import com.example.demo.service.TransactionServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import javax.persistence.EntityNotFoundException;
import java.math.BigDecimal;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class TransactionServiceImplTest {

    @Mock
    private BankAccountRepository bankAccountRepository;

    @InjectMocks
    private TransactionServiceImpl transactionService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.initMocks(this);
    }

    @Test
    void testCreateTransaction_Success() {
        BankAccount account = new BankAccount();
        account.setBalance(BigDecimal.valueOf(250));

        when(bankAccountRepository.findBySubject_Id(anyLong())).thenReturn(Optional.of(account));

        PostTransactionRequest request = new PostTransactionRequest();
        request.setAmount(BigDecimal.valueOf(50));

        assertDoesNotThrow(() -> transactionService.createTransaction(1L, request));

        verify(bankAccountRepository).findBySubject_Id(1L);
        verify(bankAccountRepository).save(account);
    }

    @Test
    void testCreateTransaction_NegativeAmount() {
        long accountId = 1L;
        PostTransactionRequest request = new PostTransactionRequest();
        request.setAmount(BigDecimal.valueOf(-50));

        assertThrows(IllegalArgumentException.class, () -> transactionService.createTransaction(accountId, request));
        verify(bankAccountRepository, never()).findBySubject_Id(accountId);
        verify(bankAccountRepository, never()).save(any(BankAccount.class));
    }

    @Test
    void testCreateTransaction_LowBalance() {
        long accountId = 1L;
        BigDecimal initialBalance = BigDecimal.valueOf(100);
        BigDecimal transactionAmount = BigDecimal.valueOf(50);
        BankAccount account = new BankAccount();
        account.setId(accountId);
        account.setBalance(initialBalance);

        when(bankAccountRepository.findBySubject_Id(accountId)).thenReturn(Optional.of(account));

        PostTransactionRequest request = new PostTransactionRequest();
        request.setAmount(transactionAmount);

        assertThrows(InternalTransactionException.class, () -> transactionService.createTransaction(accountId, request));
        verify(bankAccountRepository, never()).save(any(BankAccount.class));
    }

    @Test
    void testCreateTransaction_AccountNotFound() {
        long accountId = 1L;
        PostTransactionRequest request = new PostTransactionRequest();
        request.setAmount(BigDecimal.valueOf(50));

        when(bankAccountRepository.findBySubject_Id(accountId)).thenReturn(Optional.empty());

        assertThrows(EntityNotFoundException.class, () -> transactionService.createTransaction(accountId, request));
        verify(bankAccountRepository, never()).save(any(BankAccount.class));
    }
}

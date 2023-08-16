package com.example.demo.service;

import com.example.demo.api.PostTransactionRequest;
import com.example.demo.domain.BankAccount;
import com.example.demo.exception.InternalTransactionException;
import com.example.demo.repository.BankAccountRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import javax.persistence.EntityNotFoundException;
import java.math.BigDecimal;
import java.util.Optional;

@Service
public class TransactionServiceImpl implements TransactionService{

    private final BankAccountRepository bankAccountRepository;

    @Autowired
    public TransactionServiceImpl(BankAccountRepository bankAccountRepository) {
        this.bankAccountRepository = bankAccountRepository;
    }
    @Override
    @Transactional
    public void createTransaction(Long accountId, PostTransactionRequest request){
        if(request.getAmount().compareTo(BigDecimal.ZERO) <= 0){
            throw new IllegalArgumentException("Transaction amount must be positive");
        }

        Optional<BankAccount> optionalAccount = bankAccountRepository.findBySubject_Id(accountId);
        if(optionalAccount.isPresent()){
            BankAccount account = optionalAccount.get();
            BigDecimal newBalance = account.getBalance().add(request.getAmount());

            if (newBalance.compareTo(BigDecimal.valueOf(200)) < 0) {
                throw new InternalTransactionException();
            }

            account.setBalance(newBalance);
            bankAccountRepository.save(account);

        }else {
            throw new EntityNotFoundException("Account not found");
        }
    }
}

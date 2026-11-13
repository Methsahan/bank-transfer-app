package com.example.bank_transfer_app.service;

import com.example.bank_transfer_app.Repository.BankTransactionRepository;
import com.example.bank_transfer_app.model.BankTransaction;
import com.example.bank_transfer_app.model.TransactionStatus;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

@Service
public class TransactionAuditService {

    private final BankTransactionRepository transactionRepository;

    public TransactionAuditService(
            BankTransactionRepository transactionRepository) {
        this.transactionRepository = transactionRepository;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void recordFailure(
            String fromAccount,
            String toAccount,
            BigDecimal amount,
            String message) {

        BankTransaction transaction = new BankTransaction();

        transaction.setFromAccount(fromAccount);
        transaction.setToAccount(toAccount);
        transaction.setAmount(amount);
        transaction.setStatus(TransactionStatus.FAILED);
        transaction.setDescription(message);

        transactionRepository.save(transaction);
    }
}

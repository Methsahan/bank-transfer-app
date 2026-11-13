package com.example.bank_transfer_app.Repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.example.bank_transfer_app.model.BankTransaction;
import com.example.bank_transfer_app.model.TransactionStatus;

@Repository
public interface BankTransactionRepository
        extends JpaRepository<BankTransaction, Long> {

    // 1. All transactions, newest first
    @Query("SELECT b FROM BankTransaction b ORDER BY b.createdAt DESC")
    List<BankTransaction> findAllLatest();

    // 2. Transactions where account is sender or receiver
    @Query("""
        SELECT b FROM BankTransaction b
        WHERE b.fromAccount = :account
        OR b.toAccount = :account
        ORDER BY b.createdAt DESC
    """)
    List<BankTransaction> findHistory(
            @Param("account") String account);

    // 3. Find transactions by status
    @Query("""
        SELECT b FROM BankTransaction b
        WHERE b.status = :status
        ORDER BY b.createdAt DESC
    """)
    List<BankTransaction> findByStatusLatest(
            @Param("status") TransactionStatus status);

    // 4. Count transactions by status
    @Query("SELECT COUNT(b) FROM BankTransaction b WHERE b.status = :status")
    long countByStatus(
            @Param("status") TransactionStatus status);
}
package com.example.bank_transfer_app.Repository;

import java.math.BigDecimal;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.example.bank_transfer_app.model.Customer;

@Repository
public interface CustomerRepository
        extends JpaRepository<Customer, Long> {

    // 1. Find customer by account number
    @Query("SELECT c FROM Customer c WHERE c.accountNumber = :accountNumber")
    Customer getByAccountNumber(
            @Param("accountNumber") String accountNumber);

    // 2. Count existing account numbers
    @Query("SELECT COUNT(c) FROM Customer c WHERE c.accountNumber = :accountNumber")
    long countAccounts(
            @Param("accountNumber") String accountNumber);

    // 3. Count existing emails
    @Query("SELECT COUNT(c) FROM Customer c WHERE c.email = :email")
    long countEmails(
            @Param("email") String email);

    // 4. Find all customers ordered by name
    @Query("SELECT c FROM Customer c ORDER BY c.fullName ASC")
    List<Customer> findAllOrdered();

    // 5. Search customers by name
    @Query("""
        SELECT c FROM Customer c
        WHERE LOWER(c.fullName)
        LIKE LOWER(CONCAT('%', :keyword, '%'))
    """)
    List<Customer> searchByName(
            @Param("keyword") String keyword);

    // 6. Calculate total balance
    @Query("SELECT SUM(c.balance) FROM Customer c")
    BigDecimal getTotalBalance();

    // 7. Count customers using native SQL
    @Query(value = "SELECT COUNT(*) FROM customers", nativeQuery = true)
    long countCustomersNative();

    // 8. Debit account
    @Modifying(clearAutomatically = true)
    @Query("""
        UPDATE Customer c
        SET c.balance = c.balance - :amount
        WHERE c.accountNumber = :accountNumber
        AND c.balance >= :amount
    """)
    int debit(
            @Param("accountNumber") String accountNumber,
            @Param("amount") BigDecimal amount);

    // 9. Credit account
    @Modifying(clearAutomatically = true)
    @Query("""
        UPDATE Customer c
        SET c.balance = c.balance + :amount
        WHERE c.accountNumber = :accountNumber
    """)
    int credit(
            @Param("accountNumber") String accountNumber,
            @Param("amount") BigDecimal amount);
}
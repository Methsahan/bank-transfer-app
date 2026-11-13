package com.example.bank_transfer_app.service;

import java.math.BigDecimal;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.bank_transfer_app.Repository.CustomerRepository;
import com.example.bank_transfer_app.Repository.BankTransactionRepository;

import com.example.bank_transfer_app.exception.AccountNotFoundException;
import com.example.bank_transfer_app.exception.InsufficientBalanceException;
import com.example.bank_transfer_app.exception.InvalidTransferException;

import com.example.bank_transfer_app.model.Customer;
import com.example.bank_transfer_app.model.BankTransaction;
import com.example.bank_transfer_app.model.TransactionStatus;

@Service
@Transactional(readOnly = true)
public class BankService {

    private final CustomerRepository customerRepository;
    private final BankTransactionRepository bankTransactionRepository;

    public BankService(
            CustomerRepository customerRepository,
            BankTransactionRepository bankTransactionRepository) {

        this.customerRepository = customerRepository;
        this.bankTransactionRepository = bankTransactionRepository;
    }

    // Get all customers
    public List<Customer> getAllCustomers() {
        return customerRepository.findAllOrdered();
    }

    // Search customers
    public List<Customer> searchCustomers(String keyword) {

        if (keyword == null || keyword.isBlank()) {
            return getAllCustomers();
        }

        return customerRepository.searchByName(keyword);
    }

    // Calculate total balance
    public BigDecimal getTotalBalance() {

        BigDecimal total = customerRepository.getTotalBalance();

        return total == null ? BigDecimal.ZERO : total;
    }

    // Get latest transactions
    public List<BankTransaction> getLatestTransactions() {
        return bankTransactionRepository.findAllLatest();
    }

    // Get account transaction history
    public List<BankTransaction> getHistory(String accountNumber) {
        return bankTransactionRepository.findHistory(accountNumber);
    }

    // Count successful transfers
    public long getSuccessCount() {
        return bankTransactionRepository.countByStatus(
                TransactionStatus.SUCCESS);
    }

    // Count failed transfers
    public long getFailedCount() {
        return bankTransactionRepository.countByStatus(
                TransactionStatus.FAILED);
    }

    // Add new customer
    @Transactional
    public Customer createCustomer(Customer customer) {

        if (customerRepository.countAccounts(
                customer.getAccountNumber()) > 0) {

            throw new InvalidTransferException(
                    "Account number already exists");
        }

        if (customerRepository.countEmails(
                customer.getEmail()) > 0) {

            throw new InvalidTransferException(
                    "Email already exists");
        }

        return customerRepository.save(customer);
    }

    // Transfer money between two customers
    @Transactional(rollbackFor = Exception.class)
    public BankTransaction transfer(
            String fromAccount,
            String toAccount,
            BigDecimal amount,
            boolean simulateFailure) {

        if (fromAccount == null || toAccount == null
                || fromAccount.isBlank() || toAccount.isBlank()
                || fromAccount.equals(toAccount)) {

            throw new InvalidTransferException(
                    "Sender and receiver must be different accounts");
        }

        if (amount == null ||
                amount.compareTo(BigDecimal.ZERO) <= 0) {

            throw new InvalidTransferException(
                    "Amount must be greater than zero");
        }

        Customer sender =
                customerRepository.getByAccountNumber(fromAccount);

        if (sender == null) {
            throw new AccountNotFoundException(
                    "Sender account not found");
        }

        Customer receiver =
                customerRepository.getByAccountNumber(toAccount);

        if (receiver == null) {
            throw new AccountNotFoundException(
                    "Receiver account not found");
        }

        // Deduct money from sender
        int rowsUpdated =
                customerRepository.debit(fromAccount, amount);

        if (rowsUpdated == 0) {
            throw new InsufficientBalanceException(
                    "Insufficient balance in account " + fromAccount);
        }

        // Simulate a system failure for rollback testing
        if (simulateFailure) {
            throw new RuntimeException(
                    "All changes were rolled back");
        }

        // Add money to receiver
        int credited =
                customerRepository.credit(toAccount, amount);

        if (credited == 0) {
            throw new AccountNotFoundException(
                    "Receiver account not found");
        }

        // Save successful transfer
        BankTransaction transaction = new BankTransaction();

        transaction.setFromAccount(fromAccount);
        transaction.setToAccount(toAccount);
        transaction.setAmount(amount);
        transaction.setStatus(TransactionStatus.SUCCESS);
        transaction.setDescription("Transfer completed");

        return bankTransactionRepository.save(transaction);
    }

    // Deposit cash into a customer account
    @Transactional(rollbackFor = Exception.class)
    public BankTransaction deposit(
            String accountNumber,
            BigDecimal amount) {

        validateCashOperation(accountNumber, amount);

        Customer customer =
                customerRepository.getByAccountNumber(accountNumber);

        if (customer == null) {
            throw new AccountNotFoundException(
                    "Account not found: " + accountNumber);
        }

        // Add deposit amount to customer balance
        int updated =
                customerRepository.credit(accountNumber, amount);

        if (updated == 0) {
            throw new AccountNotFoundException(
                    "Unable to deposit into account: " + accountNumber);
        }

        // Save deposit transaction
        BankTransaction transaction = new BankTransaction();

        transaction.setFromAccount("CASH DEPOSIT");
        transaction.setToAccount(accountNumber);
        transaction.setAmount(amount);
        transaction.setStatus(TransactionStatus.SUCCESS);
        transaction.setDescription("Cash Deposit");

        return bankTransactionRepository.save(transaction);
    }

    // Withdraw cash from a customer account
    @Transactional(rollbackFor = Exception.class)
    public BankTransaction withdraw(
            String accountNumber,
            BigDecimal amount) {

        validateCashOperation(accountNumber, amount);

        Customer customer =
                customerRepository.getByAccountNumber(accountNumber);

        if (customer == null) {
            throw new AccountNotFoundException(
                    "Account not found: " + accountNumber);
        }

        // Deduct withdrawal amount from customer balance
        int updated =
                customerRepository.debit(accountNumber, amount);

        if (updated == 0) {
            throw new InsufficientBalanceException(
                    "Insufficient balance in account " + accountNumber);
        }

        // Save withdrawal transaction
        BankTransaction transaction = new BankTransaction();

        transaction.setFromAccount(accountNumber);
        transaction.setToAccount("CASH WITHDRAWAL");
        transaction.setAmount(amount);
        transaction.setStatus(TransactionStatus.SUCCESS);
        transaction.setDescription("Cash Withdrawal");

        return bankTransactionRepository.save(transaction);
    }

    // Validate deposit and withdrawal details
    private void validateCashOperation(
            String accountNumber,
            BigDecimal amount) {

        if (accountNumber == null || accountNumber.isBlank()) {
            throw new InvalidTransferException(
                    "Account number is required");
        }

        if (amount == null ||
                amount.compareTo(BigDecimal.ZERO) <= 0) {

            throw new InvalidTransferException(
                    "Amount must be greater than zero");
        }
    }
}
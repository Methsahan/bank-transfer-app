
package com.example.bank_transfer_app.controller;

import java.math.BigDecimal;
import java.util.List;

import com.example.bank_transfer_app.dto.TransferRequest;
import com.example.bank_transfer_app.model.Customer;
import com.example.bank_transfer_app.model.BankTransaction;
import com.example.bank_transfer_app.model.TransactionStatus;
import com.example.bank_transfer_app.service.BankService;
import com.example.bank_transfer_app.service.TransactionAuditService;

import jakarta.validation.Valid;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class BankController {

    private final BankService bankService;
    private final TransactionAuditService auditService;

    public BankController(
            BankService bankService,
            TransactionAuditService auditService) {

        this.bankService = bankService;
        this.auditService = auditService;
    }

    @GetMapping("/")
    public String dashboard(
            @RequestParam(required = false) String keyword,
            Model model) {

        addDashboardData(model, keyword);

        model.addAttribute("customer", new Customer());
        model.addAttribute("transferRequest", new TransferRequest());

        return "bank";
    }

    @PostMapping("/customers")
    public String addCustomer(
            @ModelAttribute("customer") Customer customer,
            RedirectAttributes redirectAttributes) {

        try {
            bankService.createCustomer(customer);

            redirectAttributes.addFlashAttribute(
                    "successMessage",
                    "Customer added successfully!");

        } catch (RuntimeException e) {

            redirectAttributes.addFlashAttribute(
                    "errorMessage", e.getMessage());
        }

        return "redirect:/";
    }

    @PostMapping("/transfer")
    public String transfer(
            @Valid @ModelAttribute("transferRequest")
            TransferRequest request,
            BindingResult result,
            Model model,
            RedirectAttributes redirectAttributes) {

        if (result.hasErrors()) {

            addDashboardData(model, null);

            model.addAttribute("customer", new Customer());
            model.addAttribute("transferRequest", request);

            return "bank";
        }

        try {

            bankService.transfer(
                    request.getFromAccount(),
                    request.getToAccount(),
                    request.getAmount(),
                    request.isSimulateFailure()
            );

            redirectAttributes.addFlashAttribute(
                    "successMessage",
                    "Transfer completed successfully!");

        } catch (RuntimeException e) {

            auditService.recordFailure(
                    request.getFromAccount(),
                    request.getToAccount(),
                    request.getAmount(),
                    e.getMessage()
            );

            redirectAttributes.addFlashAttribute(
                    "errorMessage", e.getMessage());
        }

        return "redirect:/";
    }

    // Deposit
    @PostMapping("/deposit")
    public String deposit(
            @RequestParam String accountNumber,
            @RequestParam BigDecimal amount,
            RedirectAttributes redirectAttributes) {

        try {

            bankService.deposit(accountNumber, amount);

            redirectAttributes.addFlashAttribute(
                    "successMessage",
                    "Cash deposited successfully!");

        } catch (RuntimeException e) {

            redirectAttributes.addFlashAttribute(
                    "errorMessage", e.getMessage());
        }

        return "redirect:/";
    }

    // Withdraw
    @PostMapping("/withdraw")
    public String withdraw(
            @RequestParam String accountNumber,
            @RequestParam BigDecimal amount,
            RedirectAttributes redirectAttributes) {

        try {

            bankService.withdraw(accountNumber, amount);

            redirectAttributes.addFlashAttribute(
                    "successMessage",
                    "Cash withdrawn successfully!");

        } catch (RuntimeException e) {

            redirectAttributes.addFlashAttribute(
                    "errorMessage", e.getMessage());
        }

        return "redirect:/";
    }

    // Account transaction history
    @GetMapping("/accounts/{accountNumber}/history")
    public String accountHistory(
            @PathVariable String accountNumber,
            Model model) {

        // Find customer
        Customer customer = bankService.getAllCustomers()
                .stream()
                .filter(c -> accountNumber.equals(c.getAccountNumber()))
                .findFirst()
                .orElse(null);

        if (customer == null) {
            return "redirect:/";
        }

        // Get account transactions
        List<BankTransaction> transactions =
                bankService.getHistory(accountNumber);

        // Calculate successful outgoing transfer amount
        BigDecimal totalAmountSent = transactions.stream()
                .filter(t -> t.getStatus() == TransactionStatus.SUCCESS)
                .filter(t -> accountNumber.equals(t.getFromAccount()))
                .filter(t -> !"CASH WITHDRAWAL".equals(t.getToAccount()))
                .map(BankTransaction::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        model.addAttribute("transactions", transactions);
        model.addAttribute("accountNumber", accountNumber);
        model.addAttribute("customerName", customer.getFullName());
        model.addAttribute("currentBalance", customer.getBalance());
        model.addAttribute("totalAmountSent", totalAmountSent);

        return "history";
    }

    // Dashboard data
    private void addDashboardData(Model model, String keyword) {

        model.addAttribute(
                "customers",
                bankService.searchCustomers(keyword));

        model.addAttribute(
                "totalCustomers",
                bankService.getAllCustomers().size());

        model.addAttribute(
                "totalBalance",
                bankService.getTotalBalance());

        model.addAttribute(
                "transactions",
                bankService.getLatestTransactions());

        model.addAttribute(
                "successCount",
                bankService.getSuccessCount());

        model.addAttribute(
                "failedCount",
                bankService.getFailedCount());

        model.addAttribute("keyword", keyword);
    }
}

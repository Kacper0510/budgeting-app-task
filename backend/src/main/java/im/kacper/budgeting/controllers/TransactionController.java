package im.kacper.budgeting.controllers;

import im.kacper.budgeting.models.Account;
import im.kacper.budgeting.models.Category;
import im.kacper.budgeting.models.Transaction;
import im.kacper.budgeting.models.TransactionType;
import im.kacper.budgeting.repositories.AccountRepository;
import im.kacper.budgeting.repositories.CategoryRepository;
import im.kacper.budgeting.repositories.TransactionRepository;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/accounts/{accountId}/transactions")
public class TransactionController {
    private AccountRepository accountRepository;
    private TransactionRepository transactionRepository;
    private CategoryRepository categoryRepository;

    public TransactionController(
        AccountRepository accountRepository,
        TransactionRepository transactionRepository,
        CategoryRepository categoryRepository
    ) {
        this.accountRepository = accountRepository;
        this.transactionRepository = transactionRepository;
        this.categoryRepository = categoryRepository;
    }

    private Account loadAccount(long accountId) {
        return accountRepository.findById(accountId).orElseThrow(
            () -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Account not found")
        );
    }

    public record GetTransactionsRequest(LocalDateTime from, LocalDateTime to, Long categoryId) {}

    @GetMapping
    public List<Transaction> getAllTransactions(
        @PathVariable long accountId, @ModelAttribute GetTransactionsRequest request
    ) {
        var account = loadAccount(accountId);
        if (request.from != null && request.to != null && request.from.isAfter(request.to)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "'from' date must be before 'to' date");
        }
        Category category = null;
        if (request.categoryId != null) {
            category =
                categoryRepository.findById(request.categoryId)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Category not found"));
        }
        return transactionRepository.findByFilters(account, request.from, request.to, category);
    }

    public record CreateTransactionRequest(
        @Positive(message = "Amount must be positive") long amount,
        TransactionType type,
        String description,
        long categoryId
    ) {}

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Transaction createTransaction(
        @PathVariable long accountId, @RequestBody @Valid CreateTransactionRequest request
    ) {
        var account = loadAccount(accountId);
        var category =
            categoryRepository.findById(request.categoryId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Category not found"));
        var transaction = new Transaction(request.amount, request.type, category, request.description, account);
        transactionRepository.save(transaction);
        account.addTransaction(transaction);
        accountRepository.save(account);
        return transaction;
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteTransaction(@PathVariable long accountId, @PathVariable long id) {
        var account = loadAccount(accountId);
        var transaction = transactionRepository.findById(id).orElseThrow(
            () -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Transaction not found")
        );
        if (transaction.getAccount().map(Account::getId).orElse(0L) != account.getId()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Transaction does not belong to this account");
        }
        account.removeTransaction(transaction);
        accountRepository.save(account);
        transactionRepository.delete(transaction);
    }
}

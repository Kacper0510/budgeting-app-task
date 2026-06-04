package im.kacper.budgeting.controllers;

import im.kacper.budgeting.models.Transaction;
import im.kacper.budgeting.models.TransactionType;
import im.kacper.budgeting.repositories.AccountRepository;
import im.kacper.budgeting.repositories.CategoryRepository;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/summary")
public class SummaryController {
    private AccountRepository accountRepository;
    private CategoryRepository categoryRepository;

    public SummaryController(AccountRepository accountRepository, CategoryRepository categoryRepository) {
        this.accountRepository = accountRepository;
        this.categoryRepository = categoryRepository;
    }

    public record SummaryResponse(
        Map<TransactionType, Long> total,
        Map<TransactionType, Map<Long, Long>> byCategory,
        List<Long> budgetLimitWarnings
    ) {}

    @GetMapping
    public SummaryResponse getSummary(@RequestParam(defaultValue = "30") int days) {
        if (days <= 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "'days' must be a positive integer");
        }
        var fromDate = LocalDateTime.now().minusDays(days);
        var transactions =
            accountRepository.findAll()
                .stream()
                .flatMap(account -> account.getTransactions().stream())
                .filter(tx -> tx.getTimestamp().isAfter(fromDate))
                .toList();

        var total = transactions.stream().collect(
            Collectors.groupingBy(Transaction::getType, Collectors.summingLong(Transaction::getAmount))
        );

        var byCategory = transactions.stream().collect(Collectors.groupingBy(
            Transaction::getType,
            Collectors.groupingBy(tx -> tx.getCategory().getId(), Collectors.summingLong(Transaction::getAmount))
        ));

        var warnings =
            transactions.stream()
                .collect(Collectors.groupingBy(
                    tx -> tx.getCategory().getId(), Collectors.summingLong(Transaction::getAmount)
                ))
                .entrySet()
                .stream()
                .filter(entry -> {
                    var categoryId = entry.getKey();
                    var totalAmount = entry.getValue();
                    var category = categoryRepository.findById(categoryId);
                    if (category.isEmpty()) {
                        return false;
                    }
                    var budgetLimit = category.get().getBudgetLimit();
                    return budgetLimit.isPresent() && totalAmount > budgetLimit.get();
                })
                .map(Map.Entry::getKey)
                .toList();

        return new SummaryResponse(total, byCategory, warnings);
    }
}

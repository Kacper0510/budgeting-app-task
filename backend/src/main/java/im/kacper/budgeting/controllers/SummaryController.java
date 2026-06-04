package im.kacper.budgeting.controllers;

import im.kacper.budgeting.models.Transaction;
import im.kacper.budgeting.models.TransactionType;
import im.kacper.budgeting.repositories.AccountRepository;
import jakarta.validation.constraints.Positive;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.http.HttpStatus;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/summary")
public class SummaryController {
    private AccountRepository accountRepository;

    public SummaryController(AccountRepository accountRepository) {
        this.accountRepository = accountRepository;
    }

    public record SummaryResponse(Map<TransactionType, Long> total, Map<TransactionType, Map<Long, Long>> byCategory) {}

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

        var byCategory =
            transactions.stream()
                .filter(tx -> tx.getCategory() != null)
                .collect(Collectors.groupingBy(
                    Transaction::getType,
                    Collectors
                        .groupingBy(tx -> tx.getCategory().getId(), Collectors.summingLong(Transaction::getAmount))
                ));

        return new SummaryResponse(total, byCategory);
    }
}

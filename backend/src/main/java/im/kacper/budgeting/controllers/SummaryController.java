package im.kacper.budgeting.controllers;

import im.kacper.budgeting.models.Transaction;
import im.kacper.budgeting.models.TransactionType;
import im.kacper.budgeting.repositories.AccountRepository;
import im.kacper.budgeting.repositories.CategoryRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
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
@Tag(name = "3. Financial Summary", description = "Endpoints for retrieving financial summaries and statistics")
public class SummaryController {
    private AccountRepository accountRepository;
    private CategoryRepository categoryRepository;

    public SummaryController(AccountRepository accountRepository, CategoryRepository categoryRepository) {
        this.accountRepository = accountRepository;
        this.categoryRepository = categoryRepository;
    }

    @Schema(description = "Financial summary response containing totals, category breakdowns, and budget warnings")
    public record SummaryResponse(
        @Schema(
            description = "Total amounts grouped by transaction type (INCOME/EXPENSE)",
            example = "{\"INCOME\": 5000, \"EXPENSE\": 3200}"
        ) Map<TransactionType, Long> total,

        @Schema(
            description = "Amounts grouped by transaction type and category ID",
            example = "{\"EXPENSE\": {\"1\": 1500, \"2\": 800}, \"INCOME\": {\"3\": 3000}}"
        ) Map<TransactionType, Map<Long, Long>> byCategory,

        @Schema(description = "List of category IDs that have exceeded their budget limits", example = "[1, 5, 8]")
        List<Long> budgetLimitWarnings
    ) {}

    @GetMapping
    @Operation(
        summary = "Get financial summary",
        description = "Retrieves a summary of transactions for the specified number of days, including totals by type, breakdown by category, and budget limit warnings"
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Summary retrieved successfully",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = SummaryResponse.class))
        ),
        @ApiResponse(
            responseCode = "400",
            description = "Invalid parameter (days must be positive)",
            content = @Content
        )
    })
    public SummaryResponse getSummary(
        @Parameter(description = "Number of days to look back for transactions", example = "60", required = false)
        @RequestParam(defaultValue = "30") int days
    ) {
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

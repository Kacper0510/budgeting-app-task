package im.kacper.budgeting.controllers;

import im.kacper.budgeting.models.Account;
import im.kacper.budgeting.models.Category;
import im.kacper.budgeting.models.Transaction;
import im.kacper.budgeting.models.TransactionType;
import im.kacper.budgeting.repositories.AccountRepository;
import im.kacper.budgeting.repositories.CategoryRepository;
import im.kacper.budgeting.repositories.TransactionRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import java.io.IOException;
import java.io.StringWriter;
import java.time.LocalDateTime;
import java.util.List;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
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
@Tag(name = "2. Transaction Management", description = "Endpoints for managing account transactions")
public class TransactionController {
    private AccountRepository accountRepository;
    private TransactionRepository transactionRepository;
    private CategoryRepository categoryRepository;

    private static final CSVFormat CSV_FORMAT =
        CSVFormat.DEFAULT.builder().setHeader("ID", "Amount", "Type", "Category", "Description", "Timestamp").get();

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

    @Schema(description = "Response body for transaction details")
    public record TransactionResponse(
        @Schema(description = "Unique identifier of the transaction", example = "1") long id,
        @Schema(description = "Amount of the transaction", example = "1500") long amount,
        @Schema(description = "Type of the transaction", example = "EXPENSE", allowableValues = { "INCOME", "EXPENSE" })
        TransactionType type,
        @Schema(description = "ID of the category associated with the transaction", example = "1") long category,
        @Schema(description = "Optional description of the transaction", example = "Grocery shopping")
        String description,
        @Schema(description = "Timestamp of when the transaction was created", example = "2024-01-15T14:30:00")
        LocalDateTime timestamp
    ) {
        public static TransactionResponse fromTransaction(Transaction transaction) {
            return new TransactionResponse(
                transaction.getId(),
                transaction.getAmount(),
                transaction.getType(),
                transaction.getCategory().getId(),
                transaction.getDescription().orElse(null),
                transaction.getTimestamp()
            );
        }
    }

    @Schema(description = "Request parameters for filtering transactions")
    public record GetTransactionsRequest(
        @Schema(description = "Start date for filtering transactions (inclusive)", example = "2024-01-01T00:00:00")
        LocalDateTime from,
        @Schema(description = "End date for filtering transactions (inclusive)", example = "2024-01-31T23:59:59")
        LocalDateTime to,
        @Schema(description = "Category ID to filter by", example = "1") Long categoryId
    ) {}

    @GetMapping
    @Operation(
        summary = "Get all transactions for an account",
        description = "Retrieves a list of transactions for the specified account with optional filtering by date range and category"
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Successfully retrieved transactions",
            content = @Content(
                mediaType = "application/json",
                array = @ArraySchema(schema = @Schema(implementation = TransactionResponse.class))
            )
        ),
        @ApiResponse(
            responseCode = "400",
            description = "Invalid parameters (e.g., 'from' date after 'to' date, or category not found)",
            content = @Content
        ),
        @ApiResponse(
            responseCode = "404",
            description = "Account not found",
            content = @Content
        )
    })
    public List<TransactionResponse> getAllTransactions(
        @Parameter(description = "ID of the account", example = "1", required = true)
        @PathVariable long accountId,
        @Parameter(description = "Filter parameters for transactions")
        @ModelAttribute GetTransactionsRequest request
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
        return transactionRepository.findByFilters(account, request.from, request.to, category)
            .stream()
            .map(TransactionResponse::fromTransaction)
            .toList();
    }

    @Schema(description = "Request body for creating a new transaction")
    public record CreateTransactionRequest(
        @Schema(description = "Transaction amount (must be positive)", example = "1500")
        @Positive(message = "Amount must be positive") long amount,
        @Schema(description = "Type of transaction", example = "EXPENSE", allowableValues = { "INCOME", "EXPENSE" })
        TransactionType type,
        @Schema(description = "Optional description of the transaction", example = "Grocery shopping")
        String description,
        @Schema(description = "ID of the category for this transaction", example = "1") long categoryId
    ) {}

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(
        summary = "Create a new transaction",
        description = "Creates a new transaction for the specified account"
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "201",
            description = "Transaction created successfully",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = TransactionResponse.class))
        ),
        @ApiResponse(
            responseCode = "400",
            description = "Invalid input (e.g., invalid category, non-positive amount)",
            content = @Content
        ),
        @ApiResponse(
            responseCode = "404",
            description = "Account not found",
            content = @Content
        )
    })
    public TransactionResponse createTransaction(
        @Parameter(description = "ID of the account", example = "1", required = true)
        @PathVariable long accountId,
        @RequestBody @Valid CreateTransactionRequest request
    ) {
        var account = loadAccount(accountId);
        var category =
            categoryRepository.findById(request.categoryId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Category not found"));
        var transaction = new Transaction(request.amount, request.type, category, request.description, account);
        transactionRepository.save(transaction);
        account.addTransaction(transaction);
        accountRepository.save(account);
        return TransactionResponse.fromTransaction(transaction);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(
        summary = "Delete a transaction",
        description = "Deletes a transaction by ID. The transaction must belong to the specified account."
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "204",
            description = "Transaction deleted successfully",
            content = @Content
        ),
        @ApiResponse(
            responseCode = "400",
            description = "Transaction does not belong to this account",
            content = @Content
        ),
        @ApiResponse(
            responseCode = "404",
            description = "Account or transaction not found",
            content = @Content
        )
    })
    public void deleteTransaction(
        @Parameter(description = "ID of the account", example = "1", required = true)
        @PathVariable long accountId,
        @Parameter(description = "ID of the transaction to delete", example = "1", required = true)
        @PathVariable long id
    ) {
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

    @GetMapping(value ="/export", produces = "text/csv")
    @Operation(
        summary = "Export transactions to CSV",
        description = "Exports all transactions for the specified account in CSV format"
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "CSV export generated successfully",
            content = @Content(
                mediaType = "text/csv",
                schema = @Schema(type = "string", format = "csv", description = "CSV formatted transaction data")
            )
        ),
        @ApiResponse(
            responseCode = "404",
            description = "Account not found",
            content = @Content
        ),
        @ApiResponse(
            responseCode = "500",
            description = "Failed to export transactions due to server error",
            content = @Content
        )
    })
    public String exportTransactions(
        @Parameter(description = "ID of the account", example = "1", required = true)
        @PathVariable long accountId
    ) {
        var account = loadAccount(accountId);
        var transactions = transactionRepository.findByAccount(account);
        var stringWriter = new StringWriter();
        try (var csvBuilder = new CSVPrinter(stringWriter, CSV_FORMAT)) {
            for (var tx : transactions) {
                csvBuilder.printRecord(
                    tx.getId(),
                    tx.getAmount(),
                    tx.getType(),
                    tx.getCategory().getName(),
                    tx.getDescription().orElse(null),
                    tx.getTimestamp()
                );
            }
        } catch (IOException e) {
            throw new ResponseStatusException(
                HttpStatus.INTERNAL_SERVER_ERROR, "Failed to export transactions: " + e.getMessage()
            );
        }
        return stringWriter.toString();
    }
}

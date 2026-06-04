package im.kacper.budgeting.controllers;

import im.kacper.budgeting.models.Account;
import im.kacper.budgeting.repositories.AccountRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/accounts")
@Tag(name = "1. Account Management", description = "Endpoints for managing bank accounts")
public class AccountController {
    private AccountRepository accountRepository;

    public AccountController(AccountRepository accountRepository) {
        this.accountRepository = accountRepository;
    }

    @GetMapping
    @Operation(summary = "Get all accounts", description = "Retrieves a list of all accounts in the system")
    @ApiResponse(
        responseCode = "200",
        description = "Successfully retrieved all accounts",
        content = @Content(
            mediaType = "application/json", array = @ArraySchema(schema = @Schema(implementation = Account.class))
        )
    )  // clang-format off
    public List<Account> getAllAccounts() {
        return accountRepository.findAll();
    }  // clang-format on

    public record CreateAccountRequest(
        @Schema(description = "Name of the account", example = "Savings Account")
        @NotBlank(message = "Name is required") String name,

        @Schema(description = "Initial balance of the account", example = "1000.00", nullable = true)
        Long initialBalance
    ) {}

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(
        summary = "Create a new account",
        description = "Creates a new account with the provided name and optional initial balance"
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "201",
            description = "Account created successfully",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = Account.class))
        ),
        @ApiResponse(
            responseCode = "400",
            description = "Invalid input (e.g., missing name)",
            content = @Content
        )
    })
    public Account createAccount(@RequestBody @Valid CreateAccountRequest request) {
        var account = request.initialBalance != null
            ? new Account(request.name, request.initialBalance)
            : new Account(request.name);
        return accountRepository.save(account);
    }

    @GetMapping("/{id}")
    @Operation(
        summary = "Get account by ID",
        description = "Retrieves detailed information about a specific account"
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Account found successfully",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = Account.class))
        ),
        @ApiResponse(
            responseCode = "404",
            description = "Account not found",
            content = @Content
        )
    })
    public Account getAccountDetails(
        @Parameter(description = "ID of the account to retrieve", example = "1", required = true)
        @PathVariable long id
    ) {
        return accountRepository.findById(id).orElseThrow(
            () -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Account not found")
        );
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(
        summary = "Delete an account",
        description = "Deletes an account by ID. Account must have no associated transactions."
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "204",
            description = "Account deleted successfully",
            content = @Content
        ),
        @ApiResponse(
            responseCode = "404",
            description = "Account not found",
            content = @Content
        ),
        @ApiResponse(
            responseCode = "400",
            description = "Cannot delete account with existing transactions",
            content = @Content
        )
    })
    public void deleteAccount(
        @Parameter(description = "ID of the account to delete", example = "1", required = true)
        @PathVariable long id
    ) {
        var account = accountRepository.findById(id).orElseThrow(
            () -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Account not found")
        );
        if (!account.getTransactions().isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Cannot delete account with transactions");
        }
        accountRepository.delete(account);
    }
}

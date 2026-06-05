package im.kacper.budgeting;

import im.kacper.budgeting.models.Account;
import im.kacper.budgeting.models.Category;
import im.kacper.budgeting.models.Transaction;
import im.kacper.budgeting.models.TransactionType;
import im.kacper.budgeting.repositories.AccountRepository;
import im.kacper.budgeting.repositories.CategoryRepository;
import im.kacper.budgeting.repositories.TransactionRepository;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@AutoConfigureTestDatabase
@Transactional
class TransactionControllerTests {
    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private AccountRepository accountRepository;
    @Autowired
    private CategoryRepository categoryRepository;
    @Autowired
    private TransactionRepository transactionRepository;

    private Account checkingAccount;
    private Account savingsAccount;
    private Category foodCategory;
    private Category salaryCategory;
    private Category entertainmentCategory;
    private Transaction expenseTransaction;
    private Transaction incomeTransaction;

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE_TIME;

    @BeforeEach
    void setUp() {
        transactionRepository.deleteAll();
        transactionRepository.flush();
        accountRepository.deleteAll();
        accountRepository.flush();
        categoryRepository.deleteAll();
        categoryRepository.flush();

        checkingAccount = accountRepository.saveAndFlush(new Account("Checking Account", 10_000L));
        savingsAccount = accountRepository.saveAndFlush(new Account("Savings Account", 50_000L));

        foodCategory = categoryRepository.saveAndFlush(new Category("Food"));
        salaryCategory = categoryRepository.saveAndFlush(new Category("Salary"));
        entertainmentCategory = categoryRepository.saveAndFlush(new Category("Entertainment", 50_000L));

        expenseTransaction = new Transaction(500L, TransactionType.EXPENSE, foodCategory, "Groceries", checkingAccount);
        incomeTransaction =
            new Transaction(2_000L, TransactionType.INCOME, salaryCategory, "Monthly salary", checkingAccount);
        transactionRepository.saveAndFlush(expenseTransaction);
        transactionRepository.saveAndFlush(incomeTransaction);
        checkingAccount.addTransaction(expenseTransaction);
        checkingAccount.addTransaction(incomeTransaction);
        accountRepository.saveAndFlush(checkingAccount);
    }

    @Test
    void getAllTransactions_ShouldReturnAllTransactionsForAccount() throws Exception {
        mockMvc.perform(get("/api/accounts/{accountId}/transactions", checkingAccount.getId()))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$.length()").value(2))
            .andExpect(jsonPath("$[0].amount").value(500))
            .andExpect(jsonPath("$[0].type").value("EXPENSE"))
            .andExpect(jsonPath("$[0].category").value(foodCategory.getId()))
            .andExpect(jsonPath("$[0].description").value("Groceries"))
            .andExpect(jsonPath("$[1].amount").value(2_000))
            .andExpect(jsonPath("$[1].type").value("INCOME"))
            .andExpect(jsonPath("$[1].category").value(salaryCategory.getId()))
            .andExpect(jsonPath("$[1].description").value("Monthly salary"));
    }

    @Test
    void getAllTransactions_ShouldReturnEmptyList_WhenAccountHasNoTransactions() throws Exception {
        mockMvc.perform(get("/api/accounts/{accountId}/transactions", savingsAccount.getId()))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    void getAllTransactions_ShouldReturnNotFound_WhenAccountDoesNotExist() throws Exception {
        mockMvc.perform(get("/api/accounts/9999/transactions"))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.detail").value("Account not found"));
    }

    @Test
    void getAllTransactions_ShouldFilterByDateRange() throws Exception {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime from = now.minusDays(1);
        LocalDateTime to = now.plusDays(1);

        mockMvc
            .perform(get("/api/accounts/{accountId}/transactions", checkingAccount.getId())
                         .param("from", from.format(DATE_FORMATTER))
                         .param("to", to.format(DATE_FORMATTER)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.length()").value(2));
    }

    @Test
    void getAllTransactions_ShouldReturnBadRequest_WhenFromDateAfterToDate() throws Exception {
        LocalDateTime from = LocalDateTime.now().plusDays(1);
        LocalDateTime to = LocalDateTime.now().minusDays(1);

        mockMvc
            .perform(get("/api/accounts/{accountId}/transactions", checkingAccount.getId())
                         .param("from", from.format(DATE_FORMATTER))
                         .param("to", to.format(DATE_FORMATTER)))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.detail").value("'from' date must be before 'to' date"));
    }

    @Test
    void getAllTransactions_ShouldFilterByCategoryId() throws Exception {
        mockMvc
            .perform(get("/api/accounts/{accountId}/transactions", checkingAccount.getId())
                         .param("categoryId", String.valueOf(foodCategory.getId())))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.length()").value(1))
            .andExpect(jsonPath("$[0].category").value(foodCategory.getId()))
            .andExpect(jsonPath("$[0].description").value("Groceries"));
    }

    @Test
    void getAllTransactions_ShouldFilterByCategoryIdAndDateRange() throws Exception {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime from = now.minusDays(1);
        LocalDateTime to = now.plusDays(1);

        mockMvc
            .perform(get("/api/accounts/{accountId}/transactions", checkingAccount.getId())
                         .param("from", from.format(DATE_FORMATTER))
                         .param("to", to.format(DATE_FORMATTER))
                         .param("categoryId", String.valueOf(salaryCategory.getId())))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.length()").value(1))
            .andExpect(jsonPath("$[0].category").value(salaryCategory.getId()))
            .andExpect(jsonPath("$[0].description").value("Monthly salary"));
    }

    @Test
    void getAllTransactions_ShouldReturnBadRequest_WhenCategoryNotFound() throws Exception {
        mockMvc
            .perform(get("/api/accounts/{accountId}/transactions", checkingAccount.getId()).param("categoryId", "9999"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.detail").value("Category not found"));
    }

    @Test
    void createTransaction_ShouldCreateExpenseTransaction() throws Exception {
        String requestJson = String.format("""
        {
            "amount": 750,
            "type": "EXPENSE",
            "description": "Restaurant dinner",
            "categoryId": %d
        }
        """, entertainmentCategory.getId());

        mockMvc
            .perform(post("/api/accounts/{accountId}/transactions", checkingAccount.getId())
                         .contentType(MediaType.APPLICATION_JSON)
                         .content(requestJson))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.amount").value(750))
            .andExpect(jsonPath("$.type").value("EXPENSE"))
            .andExpect(jsonPath("$.category").value(entertainmentCategory.getId()))
            .andExpect(jsonPath("$.description").value("Restaurant dinner"))
            .andExpect(jsonPath("$.timestamp").exists());

        mockMvc.perform(get("/api/accounts/{accountId}/transactions", checkingAccount.getId()))
            .andExpect(jsonPath("$.length()").value(3));
    }

    @Test
    void createTransaction_ShouldCreateIncomeTransaction_WithoutDescription() throws Exception {
        // clang-format off
        String requestJson = String.format("""
        {
            "amount": 3000,
            "type": "INCOME",
            "categoryId": %d
        }
        """, salaryCategory.getId());
        // clang-format on

        mockMvc
            .perform(post("/api/accounts/{accountId}/transactions", checkingAccount.getId())
                         .contentType(MediaType.APPLICATION_JSON)
                         .content(requestJson))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.amount").value(3_000))
            .andExpect(jsonPath("$.type").value("INCOME"))
            .andExpect(jsonPath("$.category").value(salaryCategory.getId()))
            .andExpect(jsonPath("$.description").doesNotExist())
            .andExpect(jsonPath("$.timestamp").exists());
    }

    @Test
    void createTransaction_ShouldReturnBadRequest_WhenAmountIsZero() throws Exception {
        String requestJson = String.format("""
        {
            "amount": 0,
            "type": "EXPENSE",
            "description": "Free item",
            "categoryId": %d
        }
        """, foodCategory.getId());

        mockMvc
            .perform(post("/api/accounts/{accountId}/transactions", checkingAccount.getId())
                         .contentType(MediaType.APPLICATION_JSON)
                         .content(requestJson))
            .andExpect(status().isBadRequest());
    }

    @Test
    void createTransaction_ShouldReturnBadRequest_WhenAmountIsNegative() throws Exception {
        String requestJson = String.format("""
        {
            "amount": -100,
            "type": "EXPENSE",
            "description": "Invalid amount",
            "categoryId": %d
        }
        """, foodCategory.getId());

        mockMvc
            .perform(post("/api/accounts/{accountId}/transactions", checkingAccount.getId())
                         .contentType(MediaType.APPLICATION_JSON)
                         .content(requestJson))
            .andExpect(status().isBadRequest());
    }

    @Test
    void createTransaction_ShouldReturnBadRequest_WhenCategoryDoesNotExist() throws Exception {
        // clang-format off
        String requestJson = """
        {
            "amount": 500,
            "type": "EXPENSE",
            "description": "Invalid category",
            "categoryId": 9999
        }
        """;
        // clang-format on

        mockMvc
            .perform(post("/api/accounts/{accountId}/transactions", checkingAccount.getId())
                         .contentType(MediaType.APPLICATION_JSON)
                         .content(requestJson))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.detail").value("Category not found"));
    }

    @Test
    void createTransaction_ShouldReturnNotFound_WhenAccountDoesNotExist() throws Exception {
        String requestJson = String.format("""
        {
            "amount": 500,
            "type": "EXPENSE",
            "categoryId": %d
        }
        """, foodCategory.getId());

        mockMvc
            .perform(
                post("/api/accounts/9999/transactions").contentType(MediaType.APPLICATION_JSON).content(requestJson)
            )
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.detail").value("Account not found"));
    }

    @Test
    void deleteTransaction_ShouldDeleteSuccessfully_WhenTransactionBelongsToAccount() throws Exception {
        Long transactionId = expenseTransaction.getId();

        mockMvc.perform(delete("/api/accounts/{accountId}/transactions/{id}", checkingAccount.getId(), transactionId))
            .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/accounts/{accountId}/transactions", checkingAccount.getId()))
            .andExpect(jsonPath("$.length()").value(1))
            .andExpect(jsonPath("$[0].id").value(incomeTransaction.getId()));
    }

    @Test
    void deleteTransaction_ShouldReturnNotFound_WhenTransactionDoesNotExist() throws Exception {
        mockMvc.perform(delete("/api/accounts/{accountId}/transactions/9999", checkingAccount.getId()))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.detail").value("Transaction not found"));
    }

    @Test
    void deleteTransaction_ShouldReturnNotFound_WhenAccountDoesNotExist() throws Exception {
        mockMvc.perform(delete("/api/accounts/9999/transactions/1"))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.detail").value("Account not found"));
    }

    @Test
    void deleteTransaction_ShouldReturnBadRequest_WhenTransactionDoesNotBelongToAccount() throws Exception {
        Transaction savingsTransaction =
            new Transaction(1_000L, TransactionType.INCOME, salaryCategory, "Bonus", savingsAccount);
        transactionRepository.saveAndFlush(savingsTransaction);
        savingsAccount.addTransaction(savingsTransaction);
        accountRepository.saveAndFlush(savingsAccount);

        mockMvc
            .perform(delete(
                "/api/accounts/{accountId}/transactions/{id}", checkingAccount.getId(), savingsTransaction.getId()
            ))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.detail").value("Transaction does not belong to this account"));

        mockMvc.perform(get("/api/accounts/{accountId}/transactions", savingsAccount.getId()))
            .andExpect(jsonPath("$.length()").value(1));
    }

    @Test
    void exportTransactions_ShouldReturnCSVFormat() throws Exception {
        mockMvc.perform(get("/api/accounts/{accountId}/transactions/export", checkingAccount.getId()))
            .andExpect(status().isOk())
            .andExpect(content().contentTypeCompatibleWith("text/csv"))
            .andExpect(content().string(Matchers.containsString("ID,Amount,Type,Category,Description,Timestamp")))
            .andExpect(content().string(Matchers.containsString("Groceries")))
            .andExpect(content().string(Matchers.containsString("Monthly salary")))
            .andExpect(content().string(Matchers.containsString("Food")))
            .andExpect(content().string(Matchers.containsString("Salary")));
    }

    @Test
    void exportTransactions_ShouldReturnCSVWithHeadersOnly_WhenNoTransactions() throws Exception {
        mockMvc.perform(get("/api/accounts/{accountId}/transactions/export", savingsAccount.getId()))
            .andExpect(status().isOk())
            .andExpect(content().contentTypeCompatibleWith("text/csv"))
            .andExpect(content().string(Matchers.containsString("ID,Amount,Type,Category,Description,Timestamp")))
            .andExpect(content().string(Matchers.not(Matchers.containsString("Groceries"))))
            .andExpect(content().string(Matchers.not(Matchers.containsString("Monthly salary"))));
    }

    @Test
    void exportTransactions_ShouldReturnNotFound_WhenAccountDoesNotExist() throws Exception {
        mockMvc.perform(get("/api/accounts/9999/transactions/export"))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.detail").value("Account not found"));
    }

    @Test
    void createTransaction_ShouldUpdateAccountBalance() throws Exception {
        mockMvc.perform(get("/api/accounts/{id}", savingsAccount.getId()))
            .andExpect(jsonPath("$.balance").value(50_000));

        // clang-format off
        String expenseRequest = String.format("""
        {
            "amount": 1500,
            "type": "EXPENSE",
            "categoryId": %d
        }
        """, foodCategory.getId());
        // clang-format on

        mockMvc
            .perform(post("/api/accounts/{accountId}/transactions", savingsAccount.getId())
                         .contentType(MediaType.APPLICATION_JSON)
                         .content(expenseRequest))
            .andExpect(status().isCreated());

        // clang-format off
        String incomeRequest = String.format("""
        {
            "amount": 3000,
            "type": "INCOME",
            "categoryId": %d
        }
        """, salaryCategory.getId());
        // clang-format on

        mockMvc
            .perform(post("/api/accounts/{accountId}/transactions", savingsAccount.getId())
                         .contentType(MediaType.APPLICATION_JSON)
                         .content(incomeRequest))
            .andExpect(status().isCreated());

        // Account balance should be updated (50,000 - 1,500 + 3,000 = 51,500)
        mockMvc.perform(get("/api/accounts/{id}", savingsAccount.getId()))
            .andExpect(jsonPath("$.balance").value(51_500));
    }

    @Test
    void deleteTransaction_ShouldUpdateAccountBalance() throws Exception {
        mockMvc.perform(get("/api/accounts/{id}", checkingAccount.getId()))
            .andExpect(jsonPath("$.balance").value(11_500));

        mockMvc
            .perform(delete(
                "/api/accounts/{accountId}/transactions/{id}", checkingAccount.getId(), expenseTransaction.getId()
            ))
            .andExpect(status().isNoContent());

        // Balance should be updated (11,500 + 500 = 12,000)
        mockMvc.perform(get("/api/accounts/{id}", checkingAccount.getId()))
            .andExpect(jsonPath("$.balance").value(12_000));

        mockMvc
            .perform(delete(
                "/api/accounts/{accountId}/transactions/{id}", checkingAccount.getId(), incomeTransaction.getId()
            ))
            .andExpect(status().isNoContent());

        // Balance should be updated back to original (12,000 - 2,000 = 10,000)
        mockMvc.perform(get("/api/accounts/{id}", checkingAccount.getId()))
            .andExpect(jsonPath("$.balance").value(10_000));
    }
}

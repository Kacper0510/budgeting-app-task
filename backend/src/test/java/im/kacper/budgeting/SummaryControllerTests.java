package im.kacper.budgeting;

import im.kacper.budgeting.models.Account;
import im.kacper.budgeting.models.Category;
import im.kacper.budgeting.models.Transaction;
import im.kacper.budgeting.models.TransactionType;
import im.kacper.budgeting.repositories.AccountRepository;
import im.kacper.budgeting.repositories.CategoryRepository;
import im.kacper.budgeting.repositories.TransactionRepository;
import java.time.LocalDateTime;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@AutoConfigureTestDatabase
@Transactional
class SummaryControllerTests {
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
    private Category groceriesCategory;
    private Category utilitiesCategory;

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
        groceriesCategory = categoryRepository.saveAndFlush(new Category("Groceries", 1_000L));
        utilitiesCategory = categoryRepository.saveAndFlush(new Category("Utilities", 2_000L));

        createTransaction(
            500L, TransactionType.EXPENSE, foodCategory, "Groceries", checkingAccount, LocalDateTime.now().minusDays(5)
        );
        createTransaction(
            2_000L,
            TransactionType.INCOME,
            salaryCategory,
            "Monthly salary",
            checkingAccount,
            LocalDateTime.now().minusDays(10)
        );
        createTransaction(
            1_200L,
            TransactionType.EXPENSE,
            entertainmentCategory,
            "Concert tickets",
            checkingAccount,
            LocalDateTime.now().minusDays(15)
        );
        createTransaction(
            800L,
            TransactionType.EXPENSE,
            groceriesCategory,
            "Weekly groceries",
            checkingAccount,
            LocalDateTime.now().minusDays(2)
        );
        createTransaction(
            300L,
            TransactionType.EXPENSE,
            utilitiesCategory,
            "Electric bill",
            checkingAccount,
            LocalDateTime.now().minusDays(20)
        );
        createTransaction(
            1_500L,
            TransactionType.EXPENSE,
            groceriesCategory,
            "Bulk shopping",
            checkingAccount,
            LocalDateTime.now().minusDays(7)
        );
        createTransaction(
            100L,
            TransactionType.EXPENSE,
            foodCategory,
            "Old transaction",
            checkingAccount,
            LocalDateTime.now().minusDays(100)
        );
        createTransaction(
            5_000L, TransactionType.INCOME, salaryCategory, "Bonus", savingsAccount, LocalDateTime.now().minusDays(75)
        );
    }

    private void createTransaction(
        Long amount,
        TransactionType type,
        Category category,
        String description,
        Account account,
        LocalDateTime timestamp
    ) {
        Transaction transaction = new Transaction(amount, type, category, description, account);
        transaction.setTimestamp(timestamp);
        transactionRepository.saveAndFlush(transaction);
        account.addTransaction(transaction);
        accountRepository.saveAndFlush(account);
    }

    @Test
    void getSummary_ShouldReturnDefault30DaySummary() throws Exception {
        mockMvc.perform(get("/api/summary"))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$.total.INCOME").value(2_000))  // Only salary from 10 days ago
            .andExpect(
                jsonPath("$.total.EXPENSE").value(500 + 1_200 + 800 + 300 + 1_500)
            )  // All expenses within 30 days
            .andExpect(jsonPath("$.byCategory.EXPENSE").isMap())
            .andExpect(jsonPath("$.byCategory.INCOME").isMap())
            .andExpect(jsonPath("$.budgetLimitWarnings").isArray());
    }

    @Test
    void getSummary_ShouldReturnSummaryForCustomDays() throws Exception {
        mockMvc.perform(get("/api/summary").param("days", "7"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.total.EXPENSE").value(800 + 500))  // Only last 7 days
            .andExpect(jsonPath("$.byCategory.EXPENSE").isMap())
            .andExpect(jsonPath("$.budgetLimitWarnings").isArray());
    }

    @Test
    void getSummary_ShouldReturnSummaryFor90Days() throws Exception {
        mockMvc.perform(get("/api/summary").param("days", "90"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.total.INCOME").value(2_000 + 5_000))  // Both salaries
            .andExpect(
                jsonPath("$.total.EXPENSE").value(500 + 1_200 + 800 + 300 + 1_500)
            )  // All expenses not including old
            .andExpect(jsonPath("$.byCategory.EXPENSE").isMap())
            .andExpect(jsonPath("$.budgetLimitWarnings").isArray());
    }

    @Test
    void getSummary_ShouldReturnEmptySummary_WhenNoTransactionsInDateRange() throws Exception {
        mockMvc.perform(get("/api/summary").param("days", "1"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.total.INCOME").doesNotExist())
            .andExpect(jsonPath("$.total.EXPENSE").doesNotExist())
            .andExpect(jsonPath("$.byCategory.INCOME").doesNotExist())
            .andExpect(jsonPath("$.byCategory.EXPENSE").doesNotExist())
            .andExpect(jsonPath("$.budgetLimitWarnings").isEmpty());
    }

    @Test
    void getSummary_ShouldCategorizeTransactionsByCategory() throws Exception {
        mockMvc.perform(get("/api/summary").param("days", "30"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.byCategory.EXPENSE").isMap())
            .andExpect(jsonPath("$.byCategory.EXPENSE['" + foodCategory.getId() + "']").value(500))
            .andExpect(jsonPath("$.byCategory.EXPENSE['" + entertainmentCategory.getId() + "']").value(1_200))
            .andExpect(jsonPath("$.byCategory.EXPENSE['" + groceriesCategory.getId() + "']").value(800 + 1_500))
            .andExpect(jsonPath("$.byCategory.EXPENSE['" + utilitiesCategory.getId() + "']").value(300))
            .andExpect(jsonPath("$.byCategory.INCOME['" + salaryCategory.getId() + "']").value(2_000));
    }

    @Test
    void getSummary_ShouldReturnBudgetLimitWarnings() throws Exception {
        mockMvc.perform(get("/api/summary").param("days", "30"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.budgetLimitWarnings").isArray())
            .andExpect(jsonPath("$.budgetLimitWarnings.length()").value(1))
            .andExpect(jsonPath("$.budgetLimitWarnings[0]").value(groceriesCategory.getId()));
    }

    @Test
    void getSummary_ShouldReturnMultipleBudgetLimitWarnings_WhenMultipleCategoriesExceedLimit() throws Exception {
        // Create additional transactions to make utilities exceed its 2000 limit
        createTransaction(
            1_800L,
            TransactionType.EXPENSE,
            utilitiesCategory,
            "Heating bill",
            checkingAccount,
            LocalDateTime.now().minusDays(10)
        );

        mockMvc.perform(get("/api/summary").param("days", "30"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.budgetLimitWarnings").isArray())
            .andExpect(jsonPath("$.budgetLimitWarnings.length()").value(2))
            .andExpect(jsonPath("$.budgetLimitWarnings[0]").value(groceriesCategory.getId()))
            .andExpect(jsonPath("$.budgetLimitWarnings[1]").value(utilitiesCategory.getId()));
    }

    @Test
    void getSummary_ShouldNotIncludeWarnings_WhenNoBudgetExceeded() throws Exception {
        // Delete transactions that exceed budget
        transactionRepository.deleteAll();
        accountRepository.findAll().forEach(account -> account.getTransactions().clear());
        accountRepository.flush();

        Category noBudgetCategory = categoryRepository.saveAndFlush(new Category("No Budget"));
        createTransaction(
            500L,
            TransactionType.EXPENSE,
            noBudgetCategory,
            "Test expense",
            checkingAccount,
            LocalDateTime.now().minusDays(1)
        );

        mockMvc.perform(get("/api/summary").param("days", "30"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.budgetLimitWarnings").isEmpty());
    }

    @Test
    void getSummary_ShouldIncludeTransactionsFromAllAccounts() throws Exception {
        mockMvc.perform(get("/api/summary").param("days", "30"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.total.EXPENSE").value(500 + 1_200 + 800 + 300 + 1_500))
            .andExpect(jsonPath("$.byCategory.INCOME['" + salaryCategory.getId() + "']").value(2_000));

        // Add income from savings account
        mockMvc.perform(get("/api/summary").param("days", "90"))
            .andExpect(jsonPath("$.total.INCOME").value(2_000 + 5_000))
            .andExpect(jsonPath("$.byCategory.INCOME['" + salaryCategory.getId() + "']").value(2_000 + 5_000));
    }

    @Test
    void getSummary_ShouldHandleOnlyIncomeTransactions() throws Exception {
        transactionRepository.deleteAll();
        accountRepository.findAll().forEach(account -> account.getTransactions().clear());
        accountRepository.flush();

        createTransaction(
            1_000L,
            TransactionType.INCOME,
            salaryCategory,
            "Income 1",
            checkingAccount,
            LocalDateTime.now().minusDays(1)
        );
        createTransaction(
            2_000L,
            TransactionType.INCOME,
            salaryCategory,
            "Income 2",
            checkingAccount,
            LocalDateTime.now().minusDays(2)
        );

        mockMvc.perform(get("/api/summary"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.total.INCOME").value(1_000 + 2_000))
            .andExpect(jsonPath("$.total.EXPENSE").doesNotExist())
            .andExpect(jsonPath("$.byCategory.INCOME").isMap())
            .andExpect(jsonPath("$.byCategory.EXPENSE").doesNotExist())
            .andExpect(jsonPath("$.budgetLimitWarnings").isEmpty());
    }

    @Test
    void getSummary_ShouldHandleOnlyExpenseTransactions() throws Exception {
        transactionRepository.deleteAll();
        accountRepository.findAll().forEach(account -> account.getTransactions().clear());
        accountRepository.flush();

        createTransaction(
            500L, TransactionType.EXPENSE, foodCategory, "Expense 1", checkingAccount, LocalDateTime.now().minusDays(1)
        );
        createTransaction(
            300L, TransactionType.EXPENSE, foodCategory, "Expense 2", checkingAccount, LocalDateTime.now().minusDays(2)
        );

        mockMvc.perform(get("/api/summary"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.total.INCOME").doesNotExist())
            .andExpect(jsonPath("$.total.EXPENSE").value(800))
            .andExpect(jsonPath("$.byCategory.INCOME").doesNotExist())
            .andExpect(jsonPath("$.byCategory.EXPENSE").isMap())
            .andExpect(jsonPath("$.budgetLimitWarnings").isEmpty());
    }

    @Test
    void getSummary_ShouldReturnBadRequest_WhenDaysIsZero() throws Exception {
        mockMvc.perform(get("/api/summary").param("days", "0"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.detail").value("'days' must be a positive integer"));
    }

    @Test
    void getSummary_ShouldReturnBadRequest_WhenDaysIsNegative() throws Exception {
        mockMvc.perform(get("/api/summary").param("days", "-5"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.detail").value("'days' must be a positive integer"));
    }

    @Test
    void getSummary_ShouldAcceptLargeDayValue() throws Exception {
        mockMvc.perform(get("/api/summary").param("days", "3650"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.total.INCOME").value(2_000 + 5_000))
            .andExpect(jsonPath("$.total.EXPENSE").value(500 + 1_200 + 800 + 300 + 1_500 + 100));
    }

    @Test
    void getSummary_ShouldNotIncludeWarningsForCategoriesWithoutBudget() throws Exception {
        // groceriesCategory has no budget, should not appear in warnings even if total is high
        mockMvc.perform(get("/api/summary").param("days", "30"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.budgetLimitWarnings").isArray())
            .andExpect(jsonPath("$.budgetLimitWarnings.length()").value(1))
            .andExpect(jsonPath("$.budgetLimitWarnings[0]").value(groceriesCategory.getId()))
            .andExpect(jsonPath("$.budgetLimitWarnings[0]").value(groceriesCategory.getId()));
    }

    @Test
    void getSummary_ShouldHandleMultipleTransactionsForSameCategory() throws Exception {
        // Add more transactions to food category
        createTransaction(
            200L, TransactionType.EXPENSE, foodCategory, "Snacks", checkingAccount, LocalDateTime.now().minusDays(3)
        );
        createTransaction(
            150L, TransactionType.EXPENSE, foodCategory, "Coffee", checkingAccount, LocalDateTime.now().minusDays(1)
        );

        mockMvc.perform(get("/api/summary"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.byCategory.EXPENSE['" + foodCategory.getId() + "']").value(500 + 200 + 150));
    }

    @Test
    void getSummary_ShouldReturnProperJsonStructure() throws Exception {
        mockMvc.perform(get("/api/summary"))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$.total").exists())
            .andExpect(jsonPath("$.byCategory").exists())
            .andExpect(jsonPath("$.budgetLimitWarnings").exists())
            .andExpect(jsonPath("$.total.INCOME").isNumber())
            .andExpect(jsonPath("$.byCategory.INCOME").isMap())
            .andExpect(jsonPath("$.byCategory.EXPENSE").isMap())
            .andExpect(jsonPath("$.budgetLimitWarnings").isArray());
    }
}

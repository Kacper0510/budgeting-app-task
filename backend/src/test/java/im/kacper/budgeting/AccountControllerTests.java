package im.kacper.budgeting;

import im.kacper.budgeting.models.Account;
import im.kacper.budgeting.models.Category;
import im.kacper.budgeting.models.Transaction;
import im.kacper.budgeting.models.TransactionType;
import im.kacper.budgeting.repositories.AccountRepository;
import im.kacper.budgeting.repositories.CategoryRepository;
import im.kacper.budgeting.repositories.TransactionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;
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
class AccountControllerTests {
    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private AccountRepository accountRepository;
    @Autowired
    private TransactionRepository transactionRepository;
    @Autowired
    private CategoryRepository categoryRepository;

    private Account savingsAccount;
    @SuppressWarnings("unused")
    private Account checkingAccount;
    private Account creditAccount;
    private Category incomeCategory;

    @BeforeEach
    void setUp() {
        transactionRepository.deleteAll();
        transactionRepository.flush();
        accountRepository.deleteAll();
        accountRepository.flush();
        categoryRepository.deleteAll();
        categoryRepository.flush();

        savingsAccount = accountRepository.saveAndFlush(new Account("Savings Account", 10_000L));
        checkingAccount = accountRepository.saveAndFlush(new Account("Checking Account", 5_000L));
        creditAccount = accountRepository.saveAndFlush(new Account("Credit Card", 0L));
        incomeCategory = categoryRepository.saveAndFlush(new Category("Income"));
    }

    @Test
    void getAllAccounts_ShouldReturnAllAccounts() throws Exception {
        mockMvc.perform(get("/api/accounts"))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$.length()").value(3))
            .andExpect(jsonPath("$[0].name").value("Savings Account"))
            .andExpect(jsonPath("$[0].balance").value(10_000))
            .andExpect(jsonPath("$[1].name").value("Checking Account"))
            .andExpect(jsonPath("$[1].balance").value(5_000))
            .andExpect(jsonPath("$[2].name").value("Credit Card"))
            .andExpect(jsonPath("$[2].balance").value(0));
    }

    @Test
    void createAccount_WithInitialBalance_ShouldCreateAndReturnAccount() throws Exception {
        // clang-format off
        String requestJson = """
        {
            "name": "Investment Account",
            "initialBalance": 25000
        }
        """;
        // clang-format on
        mockMvc.perform(post("/api/accounts").contentType(MediaType.APPLICATION_JSON).content(requestJson))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.name").value("Investment Account"))
            .andExpect(jsonPath("$.balance").value(25_000));

        mockMvc.perform(get("/api/accounts")).andExpect(jsonPath("$.length()").value(4));
    }

    @Test
    void createAccount_WithoutInitialBalance_ShouldCreateAccountWithZeroBalance() throws Exception {
        String requestJson = """
        {
            "name": "Emergency Fund"
        }
        """;
        mockMvc.perform(post("/api/accounts").contentType(MediaType.APPLICATION_JSON).content(requestJson))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.name").value("Emergency Fund"))
            .andExpect(jsonPath("$.balance").value(0));
    }

    @ParameterizedTest
    @NullSource
    @ValueSource(strings = {"", "   "})
    void createAccount_WithoutName_ShouldReturnBadRequest(String name) throws Exception {
        String requestJson = String.format("""
        {
            "name": %s,
            "initialBalance": 1000
        }
        """, name == null ? "null" : "\"" + name + "\"");
        mockMvc.perform(post("/api/accounts").contentType(MediaType.APPLICATION_JSON).content(requestJson))
            .andExpect(status().isBadRequest());
    }

    @Test
    void createAccount_WithNegativeInitialBalance_ShouldCreateAccountWithNegativeBalance() throws Exception {
        // clang-format off
        String requestJson = """
        {
            "name": "Overdrawn Account",
            "initialBalance": -5000
        }
        """;
        // clang-format on
        mockMvc.perform(post("/api/accounts").contentType(MediaType.APPLICATION_JSON).content(requestJson))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.name").value("Overdrawn Account"))
            .andExpect(jsonPath("$.balance").value(-5_000));
    }

    @Test
    void getAccountDetails_ShouldReturnAccount_WhenAccountExists() throws Exception {
        Long accountId = savingsAccount.getId();
        mockMvc.perform(get("/api/accounts/{id}", accountId))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$.id").value(accountId))
            .andExpect(jsonPath("$.name").value("Savings Account"))
            .andExpect(jsonPath("$.balance").value(10_000));
    }

    @Test
    void getAccountDetails_ShouldReturnNotFound_WhenAccountDoesNotExist() throws Exception {
        mockMvc.perform(get("/api/accounts/9999"))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.detail").value("Account not found"));
    }

    @Test
    void deleteAccount_ShouldDeleteSuccessfully_WhenAccountHasNoTransactions() throws Exception {
        Long accountId = creditAccount.getId();
        mockMvc.perform(delete("/api/accounts/{id}", accountId)).andExpect(status().isNoContent());

        mockMvc.perform(get("/api/accounts"))
            .andExpect(jsonPath("$.length()").value(2))
            .andExpect(jsonPath("$[?(@.name == 'Credit Card')]").doesNotExist());
    }

    @Test
    void deleteAccount_ShouldReturnNotFound_WhenAccountDoesNotExist() throws Exception {
        mockMvc.perform(delete("/api/accounts/9999"))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.detail").value("Account not found"));
    }

    @Test
    void deleteAccount_ShouldReturnBadRequest_WhenAccountHasTransactions() throws Exception {
        var transaction = new Transaction(1_000L, TransactionType.INCOME, incomeCategory, savingsAccount);
        transactionRepository.saveAndFlush(transaction);
        savingsAccount.addTransaction(transaction);
        accountRepository.saveAndFlush(savingsAccount);

        mockMvc.perform(delete("/api/accounts/{id}", savingsAccount.getId()))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.detail").value("Cannot delete account with transactions"));
    }
}

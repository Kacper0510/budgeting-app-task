package im.kacper.budgeting;

import im.kacper.budgeting.models.Category;
import im.kacper.budgeting.models.Transaction;
import im.kacper.budgeting.models.TransactionType;
import im.kacper.budgeting.repositories.CategoryRepository;
import im.kacper.budgeting.repositories.TransactionRepository;
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
class CategoryControllerTests {
    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private CategoryRepository categoryRepository;
    @Autowired
    private TransactionRepository transactionRepository;

    @SuppressWarnings("unused")
    private Category foodCategory;
    private Category transportCategory;
    private Category salaryCategory;
    @SuppressWarnings("unused")
    private Category entertainmentCategory;

    @BeforeEach
    void setUp() {
        transactionRepository.deleteAll();
        transactionRepository.flush();
        categoryRepository.deleteAll();
        categoryRepository.flush();

        foodCategory = categoryRepository.saveAndFlush(new Category("Food"));
        transportCategory = categoryRepository.saveAndFlush(new Category("Transport"));
        salaryCategory = categoryRepository.saveAndFlush(new Category("Salary"));
        entertainmentCategory = categoryRepository.saveAndFlush(new Category("Entertainment", 50_000L));
    }

    @Test
    void getAllCategories_ShouldReturnAllCategories() throws Exception {
        mockMvc.perform(get("/api/categories"))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$.length()").value(4))
            .andExpect(jsonPath("$[0].name").value("Food"))
            .andExpect(jsonPath("$[1].name").value("Transport"))
            .andExpect(jsonPath("$[2].name").value("Salary"))
            .andExpect(jsonPath("$[3].name").value("Entertainment"))
            .andExpect(jsonPath("$[3].budgetLimit").value(50_000));
    }

    @Test
    void createCategory_WithBudgetLimit_ShouldCreateAndReturnCategory() throws Exception {
        String requestJson = """
        {
            "name": "Groceries",
            "budgetLimit": 500
        }
        """;
        mockMvc.perform(post("/api/categories").contentType(MediaType.APPLICATION_JSON).content(requestJson))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.name").value("Groceries"))
            .andExpect(jsonPath("$.budgetLimit").value(500));
        mockMvc.perform(get("/api/categories")).andExpect(jsonPath("$.length()").value(5));
    }

    @Test
    void createCategory_WithoutBudgetLimit_ShouldCreateAndReturnCategory() throws Exception {
        String requestJson = """
        {
            "name": "Shopping"
        }
        """;
        mockMvc.perform(post("/api/categories").contentType(MediaType.APPLICATION_JSON).content(requestJson))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.name").value("Shopping"))
            .andExpect(jsonPath("$.budgetLimit").doesNotExist());
    }

    @Test
    void createCategory_WithEmptyName_ShouldReturnBadRequest() throws Exception {
        String requestJson = """
        {
            "name": "",
            "budgetLimit": 500
        }
        """;
        mockMvc.perform(post("/api/categories").contentType(MediaType.APPLICATION_JSON).content(requestJson))
            .andExpect(status().isBadRequest());
    }

    @Test
    void createCategory_WithNegativeBudgetLimit_ShouldReturnBadRequest() throws Exception {
        String requestJson = """
        {
            "name": "Groceries",
            "budgetLimit": -100
        }
        """;
        mockMvc.perform(post("/api/categories").contentType(MediaType.APPLICATION_JSON).content(requestJson))
            .andExpect(status().isBadRequest());
    }

    @Test
    void deleteCategory_ShouldDeleteSuccessfully_WhenValidConditionsMet() throws Exception {
        Long transportId = transportCategory.getId();
        mockMvc.perform(delete("/api/categories/{id}", transportId)).andExpect(status().isNoContent());
        mockMvc.perform(get("/api/categories"))
            .andExpect(jsonPath("$.length()").value(3))
            .andExpect(jsonPath("$[?(@.name == 'Transport')]").doesNotExist());
    }

    @Test
    void deleteCategory_ShouldReturnNotFound_WhenCategoryDoesNotExist() throws Exception {
        mockMvc.perform(delete("/api/categories/9999")).andExpect(status().isNotFound());
    }

    @Test
    void deleteCategory_ShouldReturnBadRequest_WhenOnlyOneCategoryRemains() throws Exception {
        categoryRepository.deleteAll();
        Category lastCategory = categoryRepository.save(new Category("Last One"));
        mockMvc.perform(delete("/api/categories/{id}", lastCategory.getId()))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.detail").value("At least one category must remain"));
    }

    @Test
    void deleteCategory_ShouldReturnBadRequest_WhenCategoryHasTransactions() throws Exception {
        var transaction = new Transaction(1_000L, TransactionType.INCOME, salaryCategory, null);
        transactionRepository.saveAndFlush(transaction);
        mockMvc.perform(delete("/api/categories/{id}", salaryCategory.getId()))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.detail").value("Cannot delete category with transactions"));
    }
}

package im.kacper.budgeting.controllers;

import im.kacper.budgeting.models.Category;
import im.kacper.budgeting.repositories.CategoryRepository;
import im.kacper.budgeting.repositories.TransactionRepository;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
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
@RequestMapping("/api/categories")
public class CategoryController {
    private CategoryRepository categoryRepository;
    private TransactionRepository transactionRepository;

    public CategoryController(CategoryRepository categoryRepository, TransactionRepository transactionRepository) {
        this.categoryRepository = categoryRepository;
        this.transactionRepository = transactionRepository;
    }

    @GetMapping
    public List<Category> getAllCategories() {
        return categoryRepository.findAll();
    }

    public record CreateCategoryRequest(
        @NotBlank(message = "Name is required") String name,
        @Positive(message = "Budget limit must be positive if provided") Long budgetLimit
    ) {}

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Category createCategory(@RequestBody @Valid CreateCategoryRequest request) {
        var category =
            request.budgetLimit != null ? new Category(request.name, request.budgetLimit) : new Category(request.name);
        return categoryRepository.save(category);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteCategory(@PathVariable long id) {
        var category = categoryRepository.findById(id).orElseThrow(
            () -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Category not found")
        );
        if (categoryRepository.count() <= 1) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "At least one category must remain");
        } else if (transactionRepository.countByCategory(category) > 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Cannot delete category with transactions");
        }
        categoryRepository.delete(category);
    }
}

package im.kacper.budgeting.controllers;

import im.kacper.budgeting.models.Category;
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
@Tag(name = "4. Category Management", description = "Endpoints for managing transaction categories")
public class CategoryController {
    private CategoryRepository categoryRepository;
    private TransactionRepository transactionRepository;

    public CategoryController(CategoryRepository categoryRepository, TransactionRepository transactionRepository) {
        this.categoryRepository = categoryRepository;
        this.transactionRepository = transactionRepository;
    }

    @GetMapping
    @Operation(
        summary = "Get all categories", description = "Retrieves a list of all transaction categories in the system"
    )
    @ApiResponse(
        responseCode = "200",
        description = "Successfully retrieved all categories",
        content = @Content(
            mediaType = "application/json", array = @ArraySchema(schema = @Schema(implementation = Category.class))
        )
    )  // clang-format off
    public List<Category> getAllCategories() {
        return categoryRepository.findAll();
    }  // clang-format on

    public record CreateCategoryRequest(
        @Schema(description = "Name of the category", example = "Groceries") @NotBlank(message = "Name is required")
        String name,

        @Schema(description = "Monthly budget limit for the category", example = "500.00", nullable = true)
        @Positive(message = "Budget limit must be positive if provided") Long budgetLimit
    ) {}

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(
        summary = "Create a new category",
        description = "Creates a new transaction category with the provided name and optional budget limit"
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "201",
            description = "Category created successfully",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = Category.class))
        ),
        @ApiResponse(
            responseCode = "400",
            description = "Invalid input (e.g., missing name or invalid budget limit)",
            content = @Content
        )
    })
    public Category createCategory(@RequestBody @Valid CreateCategoryRequest request) {
        var category =
            request.budgetLimit != null ? new Category(request.name, request.budgetLimit) : new Category(request.name);
        return categoryRepository.save(category);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(
        summary = "Delete a category",
        description = "Deletes a category by ID. Category must not have any associated transactions, and at least one category must remain in the system."
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "204",
            description = "Category deleted successfully",
            content = @Content
        ),
        @ApiResponse(
            responseCode = "404",
            description = "Category not found",
            content = @Content
        ),
        @ApiResponse(
            responseCode = "400",
            description = "Cannot delete category (at least one category must remain or category has transactions)",
            content = @Content
        )
    })
    public void deleteCategory(
        @Parameter(description = "ID of the category to delete", example = "1", required = true)
        @PathVariable long id
    ) {
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

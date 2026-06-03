package im.kacper.budgeting.models;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;

@Entity
@Table(name = "categories")
public class Category {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long id;

    @NotBlank(message = "Name is required")
    @Column(nullable = false, unique = true)
    private String name;

    @Positive(message = "Budget limit must be positive if provided")
    @Column(nullable = true)
    private Long budgetLimit;

    protected Category() {}

    public Category(String name) {
        this.name = name;
    }

    public Category(String name, Long budgetLimit) {
        this.name = name;
        this.budgetLimit = budgetLimit;
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Long getBudgetLimit() {
        return budgetLimit;
    }

    public void setBudgetLimit(Long budgetLimit) {
        this.budgetLimit = budgetLimit;
    }

    @Override
    public String toString() {
        return String.format("Category{id=%d, name='%s', budgetLimit=%s}", id, name, budgetLimit);
    }
}

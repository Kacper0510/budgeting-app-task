package im.kacper.budgeting;

import im.kacper.budgeting.models.Category;
import im.kacper.budgeting.repositories.CategoryRepository;
import jakarta.annotation.PostConstruct;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Component;

@Component
public class DatabaseInitializer {
    private CategoryRepository categoryRepository;

    public DatabaseInitializer(CategoryRepository categoryRepository) {
        this.categoryRepository = categoryRepository;
    }

    @PostConstruct
    @Transactional
    public void init() {
        if (categoryRepository.count() == 0) {
            categoryRepository.save(new Category("Food"));
            categoryRepository.save(new Category("Transport"));
            categoryRepository.save(new Category("Salary"));
            categoryRepository.save(new Category("Entertainment", 50_000L));  // 500.00 budget limit
        }
    }
}

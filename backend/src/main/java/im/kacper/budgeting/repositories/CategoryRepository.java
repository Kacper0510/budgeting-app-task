package im.kacper.budgeting.repositories;

import im.kacper.budgeting.models.Category;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CategoryRepository extends JpaRepository<Category, Long> {}

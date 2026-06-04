package im.kacper.budgeting.repositories;

import im.kacper.budgeting.models.Account;
import im.kacper.budgeting.models.Category;
import im.kacper.budgeting.models.Transaction;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TransactionRepository extends JpaRepository<Transaction, Long> {
    List<Transaction> findByAccountAndTimestampGreaterThanEqualAndTimestampLessThanEqualAndCategory(
        Account account, LocalDateTime from, LocalDateTime to, Category category
    );

    List<Transaction> findByAccountAndTimestampGreaterThanEqualAndTimestampLessThanEqual(
        Account account, LocalDateTime from, LocalDateTime to
    );

    List<Transaction> findByAccountAndCategoryAndTimestampGreaterThanEqual(
        Account account, Category category, LocalDateTime from
    );

    List<Transaction> findByAccountAndCategoryAndTimestampLessThanEqual(
        Account account, Category category, LocalDateTime to
    );

    List<Transaction> findByAccountAndCategory(Account account, Category category);

    List<Transaction> findByAccountAndTimestampGreaterThanEqual(Account account, LocalDateTime from);

    List<Transaction> findByAccountAndTimestampLessThanEqual(Account account, LocalDateTime to);

    List<Transaction> findByAccount(Account account);

    long countByCategory(Category category);

    default List<Transaction> findByFilters(Account account, LocalDateTime from, LocalDateTime to, Category category) {
        if (from != null && to != null && category != null) {
            return findByAccountAndTimestampGreaterThanEqualAndTimestampLessThanEqualAndCategory(
                account, from, to, category
            );
        } else if (from != null && to != null) {
            return findByAccountAndTimestampGreaterThanEqualAndTimestampLessThanEqual(account, from, to);
        } else if (category != null && from != null) {
            return findByAccountAndCategoryAndTimestampGreaterThanEqual(account, category, from);
        } else if (category != null && to != null) {
            return findByAccountAndCategoryAndTimestampLessThanEqual(account, category, to);
        } else if (category != null) {
            return findByAccountAndCategory(account, category);
        } else if (from != null) {
            return findByAccountAndTimestampGreaterThanEqual(account, from);
        } else if (to != null) {
            return findByAccountAndTimestampLessThanEqual(account, to);
        } else {
            return findByAccount(account);
        }
    }
}

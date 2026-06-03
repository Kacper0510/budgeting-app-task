package im.kacper.budgeting.repositories;

import im.kacper.budgeting.models.Transaction;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TransactionRepository extends JpaRepository<Transaction, Long> {}

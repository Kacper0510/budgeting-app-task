package im.kacper.budgeting.repositories;

import im.kacper.budgeting.models.Account;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AccountRepository extends JpaRepository<Account, Long> {}

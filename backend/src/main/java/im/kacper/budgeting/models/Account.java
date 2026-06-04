package im.kacper.budgeting.models;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "accounts")
public class Account {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long id;

    @Column(nullable = false, unique = true)
    private String name;

    @Column(nullable = false)
    private long balance = 0L;

    @OneToMany(mappedBy = "account", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @JsonIgnore
    private List<Transaction> transactions = new ArrayList<>();

    protected Account() {}

    public Account(String name) {
        this.name = name;
    }

    public Account(String name, long balance) {
        this.name = name;
        this.balance = balance;
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

    public long getBalance() {
        return balance;
    }

    public void setBalance(long balance) {
        this.balance = balance;
    }

    public List<Transaction> getTransactions() {
        return transactions;
    }

    public void addTransaction(Transaction transaction) {
        transactions.add(transaction);
        balance += transaction.getType() == TransactionType.INCOME ? transaction.getAmount() : -transaction.getAmount();
        transaction.setAccount(this);
    }

    public void removeTransaction(Transaction transaction) {
        if (transactions.remove(transaction)) {
            balance -=
                transaction.getType() == TransactionType.INCOME ? transaction.getAmount() : -transaction.getAmount();
            transaction.setAccount(null);
        }
    }

    @Override
    public String toString() {
        return String.format("Account{id=%d, name='%s', balance=%d}", id, name, balance);
    }
}

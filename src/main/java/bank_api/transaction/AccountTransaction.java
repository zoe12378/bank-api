package bank_api.transaction;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

/**
 * 對應 account_transactions 表中的一筆稽核紀錄。
 */
@Entity
@Table(name = "account_transactions")
public class AccountTransaction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "account_number")
    private String accountNumber;

    @Enumerated(EnumType.STRING)
    @Column(name = "transaction_type")
    private TransactionType transactionType;

    @Enumerated(EnumType.STRING)
    private TransactionStatus status;

    private BigDecimal amount;

    @Column(name = "balance_after")
    private BigDecimal balanceAfter;

    @Column(name = "counterparty_account_number")
    private String counterpartyAccountNumber;

    @Column(name = "failure_reason")
    private String failureReason;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    protected AccountTransaction() {
        // JPA 使用。
    }

    public AccountTransaction(
            String accountNumber,
            TransactionType transactionType,
            TransactionStatus status,
            BigDecimal amount,
            BigDecimal balanceAfter,
            String counterpartyAccountNumber,
            String failureReason) {
        this.accountNumber = accountNumber;
        this.transactionType = transactionType;
        this.status = status;
        this.amount = amount;
        this.balanceAfter = balanceAfter;
        this.counterpartyAccountNumber = counterpartyAccountNumber;
        this.failureReason = failureReason;
    }

    public Long getId() {
        return id;
    }

    public String getAccountNumber() {
        return accountNumber;
    }

    public TransactionType getTransactionType() {
        return transactionType;
    }

    public TransactionStatus getStatus() {
        return status;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public BigDecimal getBalanceAfter() {
        return balanceAfter;
    }

    public String getCounterpartyAccountNumber() {
        return counterpartyAccountNumber;
    }

    public String getFailureReason() {
        return failureReason;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    /**
     * 新交易尚未指定時間時，以建立當下時間寫入，避免 JPA 插入 NULL。
     */
    @PrePersist
    void assignCreatedAt() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
    }
}

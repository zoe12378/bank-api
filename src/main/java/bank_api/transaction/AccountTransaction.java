package bank_api.transaction;

import java.math.BigDecimal;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
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
}

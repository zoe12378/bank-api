package bank_api.account;

import java.math.BigDecimal;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * 對應 MySQL 的 accounts 資料表。
 * Entity 只描述資料如何映射；商業規則會放在之後的 service 層。
 */
@Entity
@Table(name = "accounts")
public class Account {

    @Id
    @Column(name = "account_number")
    private String accountNumber;

    @Column(name = "owner_name")
    private String ownerName;

    private BigDecimal balance;

    protected Account() {
        // JPA 需要無參數建構子來建立 Entity。
    }

    public String getAccountNumber() {
        return accountNumber;
    }

    public String getOwnerName() {
        return ownerName;
    }

    public BigDecimal getBalance() {
        return balance;
    }

    /**
     * 扣款前先檢查餘額。真正的資料庫交易控制會放在 service 層。
     */
    public void debit(BigDecimal amount) {
        if (balance.compareTo(amount) < 0) {
            throw new IllegalArgumentException("餘額不足，無法轉帳");
        }
        balance = balance.subtract(amount);
    }

    public void credit(BigDecimal amount) {
        balance = balance.add(amount);
    }
}

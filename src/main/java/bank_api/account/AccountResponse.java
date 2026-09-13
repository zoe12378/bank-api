package bank_api.account;

import java.math.BigDecimal;

/**
 * 回傳給 API 使用者的資料格式，避免直接暴露 Entity。
 */
public record AccountResponse(String accountNumber, String ownerName, BigDecimal balance) {

    public static AccountResponse from(Account account) {
        return new AccountResponse(
                account.getAccountNumber(),
                account.getOwnerName(),
                account.getBalance());
    }
}

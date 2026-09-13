package bank_api.transaction;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import com.fasterxml.jackson.annotation.JsonFormat;

/**
 * 回傳交易紀錄時使用的資料格式，不直接暴露 JPA Entity。
 */
public record TransactionResponse(
        Long id,
        TransactionType transactionType,
        TransactionStatus status,
        BigDecimal amount,
        BigDecimal balanceAfter,
        String counterpartyAccountNumber,
        String failureReason,
        @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime createdAt) {

    public static TransactionResponse from(AccountTransaction transaction) {
        return new TransactionResponse(
                transaction.getId(),
                transaction.getTransactionType(),
                transaction.getStatus(),
                transaction.getAmount(),
                transaction.getBalanceAfter(),
                transaction.getCounterpartyAccountNumber(),
                transaction.getFailureReason(),
                transaction.getCreatedAt());
    }
}

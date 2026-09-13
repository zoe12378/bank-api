package bank_api.transfer;

import java.math.BigDecimal;

public record TransferResponse(
        String fromAccountNumber,
        String toAccountNumber,
        BigDecimal amount,
        BigDecimal fromBalance,
        BigDecimal toBalance) {
}

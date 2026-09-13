package bank_api.transfer;

import java.math.BigDecimal;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * POST /api/transfers 接收的 JSON 格式。
 */
public record TransferRequest(
        @NotBlank(message = "轉出帳號不可空白") String fromAccountNumber,
        @NotBlank(message = "轉入帳號不可空白") String toAccountNumber,
        @NotNull(message = "轉帳金額不可空白")
        @DecimalMin(value = "0.01", message = "轉帳金額至少為 0.01") BigDecimal amount) {
}

package bank_api.transfer;

import java.math.BigDecimal;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * POST /api/transfers 接收的 JSON 格式。
 */
public record TransferRequest(
        @NotBlank String fromAccountNumber,
        @NotBlank String toAccountNumber,
        @NotNull @DecimalMin(value = "0.01") BigDecimal amount) {
}

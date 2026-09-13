package bank_api.account;

import java.math.BigDecimal;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

public record CreateAccountRequest(
        @NotBlank @Pattern(regexp = "[A-Z0-9]{4,20}", message = "帳號須為 4 至 20 碼的大寫英文或數字") String accountNumber,
        @NotBlank String ownerName,
        @NotBlank String ownerUsername,
        @NotNull @DecimalMin(value = "0.00", message = "開戶金額不可小於 0") BigDecimal openingBalance) {
}

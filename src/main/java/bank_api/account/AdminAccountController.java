package bank_api.account;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

/** 此 Controller 的 ADMIN 限制由 SecurityConfig 的 URL 規則執行。 */
@RestController
@RequestMapping("/api/admin/accounts")
@Tag(name = "Administration", description = "僅限 ROLE_ADMIN 建立並指派帳戶。")
@SecurityRequirement(name = "bearerAuth")
public class AdminAccountController {

    private final AccountAdministrationService accountAdministrationService;

    public AdminAccountController(AccountAdministrationService accountAdministrationService) {
        this.accountAdministrationService = accountAdministrationService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "建立帳戶並指派擁有者")
    public AccountResponse createAccount(@Valid @RequestBody CreateAccountRequest request) {
        return accountAdministrationService.createAccount(request);
    }
}

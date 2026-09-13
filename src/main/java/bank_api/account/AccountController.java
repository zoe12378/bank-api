package bank_api.account;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

/**
 * 接收 HTTP 請求並回傳 JSON。
 */
@RestController
@RequestMapping("/api/accounts")
public class AccountController {

    private final AccountRepository accountRepository;

    public AccountController(AccountRepository accountRepository) {
        this.accountRepository = accountRepository;
    }

    @GetMapping
    public List<AccountResponse> getMyAccounts(Authentication authentication) {
        return accountRepository.findByUserUsername(authentication.getName())
                .stream()
                .map(AccountResponse::from)
                .toList();
    }

    /**
     * 依帳號取得單一帳戶。
     * 路徑中的 {accountNumber} 會由 Spring 自動帶入方法參數。
     */
    @GetMapping("/{accountNumber}")
    public AccountResponse getMyAccount(
            @PathVariable String accountNumber,
            Authentication authentication) {
        return accountRepository.findByAccountNumberAndUserUsername(accountNumber, authentication.getName())
                .map(AccountResponse::from)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "找不到你的帳號：" + accountNumber));
    }
}

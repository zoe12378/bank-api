package bank_api.account;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import bank_api.transaction.AccountTransactionRepository;
import bank_api.transaction.TransactionResponse;

/**
 * 接收 HTTP 請求並回傳 JSON。
 */
@RestController
@RequestMapping("/api/accounts")
public class AccountController {

    private final AccountRepository accountRepository;
    private final AccountTransactionRepository transactionRepository;

    public AccountController(
            AccountRepository accountRepository,
            AccountTransactionRepository transactionRepository) {
        this.accountRepository = accountRepository;
        this.transactionRepository = transactionRepository;
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

    /**
     * 交易紀錄屬於敏感資料，必須先確認帳戶是登入者所擁有。
     */
    @GetMapping("/{accountNumber}/transactions")
    public List<TransactionResponse> getMyTransactions(
            @PathVariable String accountNumber,
            Authentication authentication) {
        accountRepository.findByAccountNumberAndUserUsername(accountNumber, authentication.getName())
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "找不到你的帳號：" + accountNumber));

        return transactionRepository.findByAccountNumberOrderByCreatedAtDescIdDesc(accountNumber)
                .stream()
                .map(TransactionResponse::from)
                .toList();
    }
}

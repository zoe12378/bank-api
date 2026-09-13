package bank_api.account;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import bank_api.transaction.AccountTransactionRepository;
import bank_api.transaction.TransactionPageResponse;

/**
 * 接收 HTTP 請求並回傳 JSON。
 */
@RestController
@RequestMapping("/api/accounts")
@Validated
@Tag(name = "Accounts", description = "登入者可讀取自己擁有的帳戶與交易紀錄。")
@SecurityRequirement(name = "bearerAuth")
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
    @Operation(summary = "取得我的帳戶")
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
    @Operation(summary = "取得我的單一帳戶")
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
    @Operation(summary = "取得我的交易紀錄", description = "可用 page、size、from、to 進行分頁與日期篩選。")
    public TransactionPageResponse getMyTransactions(
            @PathVariable String accountNumber,
            Authentication authentication,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        accountRepository.findByAccountNumberAndUserUsername(accountNumber, authentication.getName())
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "找不到你的帳號：" + accountNumber));

        if (from != null && to != null && from.isAfter(to)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "起始日期不可晚於結束日期");
        }

        LocalDateTime fromTime = from == null ? null : from.atStartOfDay();
        // 結束日期採用「隔天 00:00 前」，可完整包含該日期的所有交易。
        LocalDateTime toTime = to == null ? null : to.plusDays(1).atStartOfDay();
        Pageable pageable = PageRequest.of(page, size, Sort.by(
                Sort.Order.desc("createdAt"), Sort.Order.desc("id")));

        return TransactionPageResponse.from(
                transactionRepository.findHistory(accountNumber, fromTime, toTime, pageable));
    }
}

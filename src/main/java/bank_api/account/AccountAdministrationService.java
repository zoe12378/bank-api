package bank_api.account;

import java.math.RoundingMode;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import bank_api.auth.AppUser;
import bank_api.auth.AppUserRepository;
import bank_api.transaction.AccountTransaction;
import bank_api.transaction.AccountTransactionRepository;
import bank_api.transaction.TransactionStatus;
import bank_api.transaction.TransactionType;

/** 管理者建立帳戶時，同步留下不可缺少的開戶稽核紀錄。 */
@Service
public class AccountAdministrationService {

    private final AccountRepository accountRepository;
    private final AppUserRepository userRepository;
    private final AccountTransactionRepository transactionRepository;

    public AccountAdministrationService(
            AccountRepository accountRepository,
            AppUserRepository userRepository,
            AccountTransactionRepository transactionRepository) {
        this.accountRepository = accountRepository;
        this.userRepository = userRepository;
        this.transactionRepository = transactionRepository;
    }

    @Transactional
    public AccountResponse createAccount(CreateAccountRequest request) {
        if (accountRepository.existsById(request.accountNumber())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "帳號已存在：" + request.accountNumber());
        }

        AppUser owner = userRepository.findByUsername(request.ownerUsername())
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "找不到使用者：" + request.ownerUsername()));
        var openingBalance = request.openingBalance().setScale(2, RoundingMode.HALF_UP);
        Account account = accountRepository.save(new Account(
                request.accountNumber(), request.ownerName(), openingBalance, owner));
        transactionRepository.save(new AccountTransaction(
                account.getAccountNumber(),
                TransactionType.OPEN_ACCOUNT,
                TransactionStatus.SUCCESS,
                openingBalance,
                openingBalance,
                null,
                null));
        return AccountResponse.from(account);
    }
}

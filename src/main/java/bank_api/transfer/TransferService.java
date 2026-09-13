package bank_api.transfer;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import bank_api.account.Account;
import bank_api.account.AccountRepository;
import bank_api.transaction.AccountTransaction;
import bank_api.transaction.AccountTransactionRepository;
import bank_api.transaction.TransactionStatus;
import bank_api.transaction.TransactionType;

/**
 * 放置轉帳商業規則與資料庫 transaction 的 service 層。
 */
@Service
public class TransferService {

    private final AccountRepository accountRepository;
    private final AccountTransactionRepository transactionRepository;

    public TransferService(
            AccountRepository accountRepository,
            AccountTransactionRepository transactionRepository) {
        this.accountRepository = accountRepository;
        this.transactionRepository = transactionRepository;
    }

    /**
     * @Transactional 確保扣款、入帳及兩筆紀錄會一起提交；若方法中拋出例外則一起 rollback。
     */
    @Transactional
    public TransferResponse transfer(TransferRequest request, String username) {
        if (request.fromAccountNumber().equals(request.toAccountNumber())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "不可轉帳給同一帳戶");
        }

        Account sender = findAccount(request.fromAccountNumber());
        Account receiver = findAccount(request.toAccountNumber());
        BigDecimal amount = request.amount().setScale(2, RoundingMode.HALF_UP);

        // 即使知道帳號，也只能從自己擁有的帳戶扣款。
        if (!sender.belongsTo(username)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "你沒有使用此轉出帳號的權限");
        }

        try {
            sender.debit(amount);
        } catch (IllegalArgumentException exception) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, exception.getMessage());
        }
        receiver.credit(amount);

        transactionRepository.saveAll(List.of(
                new AccountTransaction(
                        sender.getAccountNumber(),
                        TransactionType.TRANSFER_OUT,
                        TransactionStatus.SUCCESS,
                        amount,
                        sender.getBalance(),
                        receiver.getAccountNumber(),
                        null),
                new AccountTransaction(
                        receiver.getAccountNumber(),
                        TransactionType.TRANSFER_IN,
                        TransactionStatus.SUCCESS,
                        amount,
                        receiver.getBalance(),
                        sender.getAccountNumber(),
                        null)));

        return new TransferResponse(
                sender.getAccountNumber(),
                receiver.getAccountNumber(),
                amount,
                sender.getBalance(),
                receiver.getBalance());
    }

    private Account findAccount(String accountNumber) {
        return accountRepository.findById(accountNumber)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "找不到帳號：" + accountNumber));
    }
}

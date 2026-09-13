package bank_api.transfer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import bank_api.account.Account;
import bank_api.account.AccountRepository;
import bank_api.auth.AppUser;
import bank_api.transaction.AccountTransactionRepository;

/**
 * 單元測試只驗證轉帳規則，不啟動 Spring、不連 MySQL。
 */
@ExtendWith(MockitoExtension.class)
class TransferServiceTest {

    @Mock
    private AccountRepository accountRepository;

    @Mock
    private AccountTransactionRepository transactionRepository;

    private TransferService transferService;
    private Account aliceAccount;
    private Account bobAccount;

    @BeforeEach
    void setUp() {
        transferService = new TransferService(accountRepository, transactionRepository);

        AppUser aliceUser = new AppUser("alice_user", "not-a-real-password-hash");
        aliceAccount = new Account("A001", "Alice", new BigDecimal("1000.00"), aliceUser);
        bobAccount = new Account("B001", "Bob", new BigDecimal("650.00"), null);

        when(accountRepository.findByIdForUpdate("A001")).thenReturn(Optional.of(aliceAccount));
        when(accountRepository.findByIdForUpdate("B001")).thenReturn(Optional.of(bobAccount));
    }

    @Test
    void transfer_whenSenderOwnsAccount_movesMoneyAndSavesTwoRecords() {
        TransferResponse response = transferService.transfer(
                new TransferRequest("A001", "B001", new BigDecimal("10.00")),
                "alice_user");

        assertThat(response.fromBalance()).isEqualByComparingTo("990.00");
        assertThat(response.toBalance()).isEqualByComparingTo("660.00");
        verify(accountRepository).findByIdForUpdate("A001");
        verify(accountRepository).findByIdForUpdate("B001");
        verify(transactionRepository).saveAll(any());
    }

    @Test
    void transfer_whenSenderDoesNotOwnAccount_returnsForbiddenAndDoesNotWriteRecords() {
        assertThatThrownBy(() -> transferService.transfer(
                new TransferRequest("B001", "A001", new BigDecimal("10.00")),
                "alice_user"))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(exception -> assertThat(((ResponseStatusException) exception).getStatusCode())
                        .isEqualTo(HttpStatus.FORBIDDEN));

        verify(transactionRepository, never()).saveAll(any());
    }

    @Test
    void transfer_whenBalanceIsInsufficient_returnsBadRequestAndDoesNotWriteRecords() {
        Account lowBalanceAccount = new Account(
                "A001", "Alice", new BigDecimal("5.00"), new AppUser("alice_user", "hash"));
        when(accountRepository.findByIdForUpdate("A001")).thenReturn(Optional.of(lowBalanceAccount));

        assertThatThrownBy(() -> transferService.transfer(
                new TransferRequest("A001", "B001", new BigDecimal("10.00")),
                "alice_user"))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(exception -> assertThat(((ResponseStatusException) exception).getStatusCode())
                        .isEqualTo(HttpStatus.BAD_REQUEST));

        assertThat(lowBalanceAccount.getBalance()).isEqualByComparingTo("5.00");
        verify(transactionRepository, never()).saveAll(any());
    }
}

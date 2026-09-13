package bank_api.transaction;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AccountTransactionRepository extends JpaRepository<AccountTransaction, Long> {

    List<AccountTransaction> findByAccountNumberOrderByCreatedAtDescIdDesc(String accountNumber);
}

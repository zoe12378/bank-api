package bank_api.account;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

/**
 * Spring Data JPA 會自動提供 findAll、findById 等基本資料庫操作。
 */
public interface AccountRepository extends JpaRepository<Account, String> {

    List<Account> findByUserUsername(String username);

    Optional<Account> findByAccountNumberAndUserUsername(String accountNumber, String username);
}

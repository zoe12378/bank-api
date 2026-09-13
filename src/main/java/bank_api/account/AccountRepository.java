package bank_api.account;

import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Spring Data JPA 會自動提供 findAll、findById 等基本資料庫操作。
 */
public interface AccountRepository extends JpaRepository<Account, String> {
}

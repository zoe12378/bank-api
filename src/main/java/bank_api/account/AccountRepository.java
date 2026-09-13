package bank_api.account;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import jakarta.persistence.LockModeType;

import java.util.List;
import java.util.Optional;

/**
 * Spring Data JPA 會自動提供 findAll、findById 等基本資料庫操作。
 */
public interface AccountRepository extends JpaRepository<Account, String> {

    List<Account> findByUserUsername(String username);

    Optional<Account> findByAccountNumberAndUserUsername(String accountNumber, String username);

    /**
     * 在交易完成前鎖住該帳戶列，Hibernate 會對 MySQL 發出 SELECT ... FOR UPDATE。
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT account FROM Account account WHERE account.accountNumber = :accountNumber")
    Optional<Account> findByIdForUpdate(@Param("accountNumber") String accountNumber);
}

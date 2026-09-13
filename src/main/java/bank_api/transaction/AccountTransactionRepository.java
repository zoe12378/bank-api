package bank_api.transaction;

import java.time.LocalDateTime;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface AccountTransactionRepository extends JpaRepository<AccountTransaction, Long> {

    @Query("""
            SELECT entry
            FROM AccountTransaction entry
            WHERE entry.accountNumber = :accountNumber
              AND (:fromTime IS NULL OR entry.createdAt >= :fromTime)
              AND (:toTime IS NULL OR entry.createdAt < :toTime)
            """)
    Page<AccountTransaction> findHistory(
            @Param("accountNumber") String accountNumber,
            @Param("fromTime") LocalDateTime fromTime,
            @Param("toTime") LocalDateTime toTime,
            Pageable pageable);
}

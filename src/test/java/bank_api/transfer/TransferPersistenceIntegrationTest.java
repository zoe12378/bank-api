package bank_api.transfer;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import bank_api.account.Account;
import bank_api.account.AccountRepository;
import bank_api.auth.AppUser;
import bank_api.auth.AppUserRepository;
import bank_api.security.JwtService;
import bank_api.transaction.AccountTransactionRepository;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 真正啟動 MySQL 容器，驗證 JPA、MySQL 鎖定與轉帳資料寫入能一起運作。
 * 容器與其中的資料會在測試結束後自動移除，不會碰到本機 bank_demo。
 */
@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers(disabledWithoutDocker = true)
class TransferPersistenceIntegrationTest {

    @Container
    private static final MySQLContainer<?> mysql = new MySQLContainer<>(
            DockerImageName.parse("mysql:8.0.46"))
            .withDatabaseName("bank_test")
            .withUsername("test_user")
            .withPassword("test_password");

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", mysql::getJdbcUrl);
        registry.add("spring.datasource.username", mysql::getUsername);
        registry.add("spring.datasource.password", mysql::getPassword);
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "create-drop");
        registry.add("jwt.secret", () -> "test-only-secret-must-be-at-least-32-characters-long");
    }

    @Autowired
    private TransferService transferService;

    @Autowired
    private AppUserRepository userRepository;

    @Autowired
    private AccountRepository accountRepository;

    @Autowired
    private AccountTransactionRepository transactionRepository;

    @Autowired
    private JwtService jwtService;

    @Autowired
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        transactionRepository.deleteAll();
        accountRepository.deleteAll();
        userRepository.deleteAll();

        AppUser alice = userRepository.save(new AppUser("alice_user", "not-a-real-password-hash"));
        accountRepository.saveAllAndFlush(List.of(
                new Account("A001", "Alice", new BigDecimal("1000.00"), alice),
                new Account("B001", "Bob", new BigDecimal("650.00"), null)));
    }

    @Test
    void transfer_persistsBalancesAndTwoAuditRecordsInMySql() {
        TransferResponse response = transferService.transfer(
                new TransferRequest("A001", "B001", new BigDecimal("10.00")),
                "alice_user");

        assertThat(response.fromBalance()).isEqualByComparingTo("990.00");
        assertThat(response.toBalance()).isEqualByComparingTo("660.00");
        assertThat(accountRepository.findById("A001").orElseThrow().getBalance())
                .isEqualByComparingTo("990.00");
        assertThat(accountRepository.findById("B001").orElseThrow().getBalance())
                .isEqualByComparingTo("660.00");
        assertThat(transactionRepository.count()).isEqualTo(2);
    }

    @Test
    void admin_canCreateAccountAndOpeningAuditRecord() throws Exception {
        AppUser admin = userRepository.save(new AppUser("admin_user", "not-a-real-password-hash"));
        admin.promoteToAdmin();

        mockMvc.perform(post("/api/admin/accounts")
                .header("Authorization", "Bearer " + jwtService.generateToken(admin))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {
                          "accountNumber": "C001",
                          "ownerName": "Carol",
                          "ownerUsername": "alice_user",
                          "openingBalance": 800.00
                        }
                        """))
                .andExpect(status().isCreated());

        assertThat(accountRepository.findById("C001")).isPresent();
        assertThat(transactionRepository.count()).isEqualTo(1);
    }
}

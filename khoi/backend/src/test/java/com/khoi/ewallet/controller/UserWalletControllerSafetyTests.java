package com.khoi.ewallet.controller;

import org.junit.jupiter.api.Test;
import org.springframework.aop.framework.ProxyFactory;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.annotation.AnnotationTransactionAttributeSource;
import org.springframework.transaction.interceptor.TransactionInterceptor;
import org.springframework.transaction.support.AbstractPlatformTransactionManager;
import org.springframework.transaction.support.DefaultTransactionStatus;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.locks.ReentrantLock;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class UserWalletControllerSafetyTests {

    private static final String TOKEN = "Bearer demo-token-1";

    @Test
    void successfulTransferUpdatesBothWalletsAndCreatesTransaction() {
        LedgerJdbcTemplate jdbc = new LedgerJdbcTemplate(new BigDecimal("100.00"));
        UserWalletController controller = new UserWalletController(jdbc);

        ResponseEntity<Map<String, Object>> response = controller.transferMoney(
                TOKEN, new UserWalletController.TransferRequest("0987654321", new BigDecimal("25.00"), "Kiểm tra chuyển tiền")
        );
        jdbc.releaseTransactionLock();

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(new BigDecimal("75.00"), response.getBody().get("balance"));
        assertEquals(new BigDecimal("75.00"), jdbc.senderBalance);
        assertEquals(new BigDecimal("25.00"), jdbc.receiverCredits);
        assertEquals(1, jdbc.transactionsInserted);
    }

    @Test
    void insufficientBalanceDoesNotMutateWalletsOrCreateTransaction() {
        LedgerJdbcTemplate jdbc = new LedgerJdbcTemplate(new BigDecimal("20.00"));
        UserWalletController controller = new UserWalletController(jdbc);

        ResponseEntity<Map<String, Object>> response = controller.transferMoney(
                TOKEN, new UserWalletController.TransferRequest("0987654321", new BigDecimal("25.00"), null)
        );
        jdbc.releaseTransactionLock();

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertEquals(new BigDecimal("20.00"), jdbc.senderBalance);
        assertEquals(BigDecimal.ZERO, jdbc.receiverCredits);
        assertEquals(0, jdbc.transactionsInserted);
    }

    @Test
    void paymentForInactiveServiceIsRejectedBeforeWalletMutation() {
        InactiveServiceJdbcTemplate jdbc = new InactiveServiceJdbcTemplate();
        UserWalletController controller = new UserWalletController(jdbc);

        ResponseEntity<Map<String, Object>> response = controller.payService(
                TOKEN, new UserWalletController.PaymentRequest(9, null)
        );

        assertEquals(HttpStatus.CONFLICT, response.getStatusCode());
        assertEquals(0, jdbc.updateCalls);
    }

    @Test
    void blockedUserCannotAccessWalletApi() {
        BlockedUserJdbcTemplate jdbc = new BlockedUserJdbcTemplate();
        UserWalletController controller = new UserWalletController(jdbc);

        ResponseEntity<Map<String, Object>> response = controller.depositMoney(
                TOKEN, new UserWalletController.DepositRequest(BigDecimal.ONE, null)
        );

        assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
        assertEquals("ACCOUNT_BLOCKED", response.getBody().get("code"));
        assertEquals(0, jdbc.updateCalls);
    }

    @Test
    void vietnameseServiceTextIsPreservedInApiResponse() {
        VietnameseServiceJdbcTemplate jdbc = new VietnameseServiceJdbcTemplate();
        UserWalletController controller = new UserWalletController(jdbc);

        ResponseEntity<Map<String, Object>> response = controller.getActiveServices(TOKEN);
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> services = (List<Map<String, Object>>) response.getBody().get("services");

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals("Mua thẻ điện thoại", services.get(0).get("name"));
        assertEquals("Thanh toán mô phỏng dịch vụ thẻ điện thoại", services.get(0).get("description"));
    }

    @Test
    void transactionBoundaryRollsBackWhenWalletOperationFails() {
        FailingDepositJdbcTemplate jdbc = new FailingDepositJdbcTemplate();
        RecordingTransactionManager transactionManager = new RecordingTransactionManager();
        UserWalletController target = new UserWalletController(jdbc);
        TransactionInterceptor interceptor = new TransactionInterceptor(
                transactionManager, new AnnotationTransactionAttributeSource()
        );
        ProxyFactory proxyFactory = new ProxyFactory(target);
        proxyFactory.addAdvice(interceptor);
        UserWalletController controller = (UserWalletController) proxyFactory.getProxy();

        assertThrows(DataAccessResourceFailureException.class, () -> controller.depositMoney(
                TOKEN, new UserWalletController.DepositRequest(new BigDecimal("10.00"), null)
        ));

        assertEquals(1, transactionManager.begins);
        assertEquals(0, transactionManager.commits);
        assertEquals(1, transactionManager.rollbacks);
    }

    @Test
    void concurrentTransfersCannotProduceNegativeSenderBalance() throws Exception {
        LedgerJdbcTemplate jdbc = new LedgerJdbcTemplate(new BigDecimal("100.00"));
        UserWalletController controller = new UserWalletController(jdbc);
        CountDownLatch start = new CountDownLatch(1);
        ExecutorService executor = Executors.newFixedThreadPool(2);
        try {
            Future<ResponseEntity<Map<String, Object>>> first = executor.submit(() -> transferAfter(start, controller, jdbc));
            Future<ResponseEntity<Map<String, Object>>> second = executor.submit(() -> transferAfter(start, controller, jdbc));
            start.countDown();

            List<HttpStatus> statuses = List.of(
                    (HttpStatus) first.get().getStatusCode(),
                    (HttpStatus) second.get().getStatusCode()
            );
            assertTrue(statuses.contains(HttpStatus.OK));
            assertTrue(statuses.contains(HttpStatus.BAD_REQUEST));
            assertEquals(new BigDecimal("20.00"), jdbc.senderBalance);
            assertTrue(jdbc.senderBalance.signum() >= 0);
            assertEquals(1, jdbc.transactionsInserted);
        } finally {
            executor.shutdownNow();
        }
    }

    private ResponseEntity<Map<String, Object>> transferAfter(
            CountDownLatch start, UserWalletController controller, LedgerJdbcTemplate jdbc
    ) throws Exception {
        start.await();
        try {
            return controller.transferMoney(
                    TOKEN, new UserWalletController.TransferRequest("0987654321", new BigDecimal("80.00"), null)
            );
        } finally {
            jdbc.releaseTransactionLock();
        }
    }

    private static Map<String, Object> activeUser() {
        return Map.of("id", 1, "role", "user", "status", "active");
    }

    private static class LedgerJdbcTemplate extends JdbcTemplate {
        private final ReentrantLock senderLock = new ReentrantLock();
        private final ThreadLocal<Boolean> lockHeld = ThreadLocal.withInitial(() -> false);
        private BigDecimal senderBalance;
        private BigDecimal receiverCredits = BigDecimal.ZERO;
        private int transactionsInserted;

        private LedgerJdbcTemplate(BigDecimal senderBalance) {
            this.senderBalance = senderBalance;
        }

        @Override
        public List<Map<String, Object>> queryForList(String sql, Object... args) {
            if (sql.contains("SELECT id, role, status FROM users")) return List.of(activeUser());
            if (sql.contains("WHERE u.phone = ?")) {
                return List.of(Map.of(
                        "id", 2, "phone", "0987654321", "status", "active",
                        "wallet_id", 22, "balance", receiverCredits
                ));
            }
            if (sql.contains("FROM wallets WHERE user_id = ? FOR UPDATE")) {
                senderLock.lock();
                lockHeld.set(true);
                return List.of(Map.of("id", 11, "balance", senderBalance));
            }
            throw new AssertionError("Unexpected query: " + sql);
        }

        @Override
        public int update(String sql, Object... args) {
            if (sql.startsWith("UPDATE wallets SET balance = balance -")) {
                senderBalance = senderBalance.subtract((BigDecimal) args[0]);
                return 1;
            }
            if (sql.startsWith("UPDATE wallets SET balance = balance +")) {
                receiverCredits = receiverCredits.add((BigDecimal) args[0]);
                return 1;
            }
            if (sql.contains("INSERT INTO transactions")) {
                transactionsInserted++;
                return 1;
            }
            throw new AssertionError("Unexpected update: " + sql);
        }

        private void releaseTransactionLock() {
            if (lockHeld.get()) {
                lockHeld.set(false);
                senderLock.unlock();
            }
        }
    }

    private static class InactiveServiceJdbcTemplate extends JdbcTemplate {
        private int updateCalls;

        @Override
        public List<Map<String, Object>> queryForList(String sql, Object... args) {
            if (sql.contains("SELECT id, role, status FROM users")) return List.of(activeUser());
            if (sql.contains("FROM services")) {
                return List.of(Map.of(
                        "id", 9, "name", "Dịch vụ tạm dừng", "price", BigDecimal.TEN,
                        "description", "Kiểm tra UTF-8", "is_active", false
                ));
            }
            throw new AssertionError("Unexpected query: " + sql);
        }

        @Override
        public int update(String sql, Object... args) {
            updateCalls++;
            return 1;
        }
    }

    private static class BlockedUserJdbcTemplate extends JdbcTemplate {
        private int updateCalls;

        @Override
        public List<Map<String, Object>> queryForList(String sql, Object... args) {
            return List.of(Map.of("id", 1, "role", "user", "status", "blocked"));
        }

        @Override
        public int update(String sql, Object... args) {
            updateCalls++;
            return 1;
        }
    }

    private static class FailingDepositJdbcTemplate extends JdbcTemplate {
        @Override
        public List<Map<String, Object>> queryForList(String sql, Object... args) {
            if (sql.contains("SELECT id, role, status FROM users")) return List.of(activeUser());
            if (sql.contains("FROM wallets WHERE user_id = ? FOR UPDATE")) {
                return List.of(Map.of("id", 11, "balance", new BigDecimal("50.00")));
            }
            throw new AssertionError("Unexpected query: " + sql);
        }

        @Override
        public int update(String sql, Object... args) {
            if (sql.contains("INSERT INTO transactions")) {
                throw new DataAccessResourceFailureException("Simulated transaction insert failure");
            }
            return 1;
        }
    }

    private static class VietnameseServiceJdbcTemplate extends JdbcTemplate {
        @Override
        public List<Map<String, Object>> queryForList(String sql) {
            return vietnameseServices(sql);
        }

        @Override
        public List<Map<String, Object>> queryForList(String sql, Object... args) {
            if (sql.contains("SELECT id, role, status FROM users")) return List.of(activeUser());
            return vietnameseServices(sql);
        }

        private List<Map<String, Object>> vietnameseServices(String sql) {
            if (sql.contains("FROM services")) {
                return List.of(Map.of(
                        "id", 1,
                        "name", "Mua thẻ điện thoại",
                        "price", BigDecimal.TEN,
                        "description", "Thanh toán mô phỏng dịch vụ thẻ điện thoại",
                        "is_active", true
                ));
            }
            throw new AssertionError("Unexpected query: " + sql);
        }
    }

    private static class RecordingTransactionManager extends AbstractPlatformTransactionManager {
        private int begins;
        private int commits;
        private int rollbacks;

        @Override
        protected Object doGetTransaction() {
            return new Object();
        }

        @Override
        protected void doBegin(Object transaction, TransactionDefinition definition) {
            begins++;
        }

        @Override
        protected void doCommit(DefaultTransactionStatus status) {
            commits++;
        }

        @Override
        protected void doRollback(DefaultTransactionStatus status) {
            rollbacks++;
        }
    }
}

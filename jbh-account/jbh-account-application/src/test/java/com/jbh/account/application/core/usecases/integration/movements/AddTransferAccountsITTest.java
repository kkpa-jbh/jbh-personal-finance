package com.jbh.account.application.core.usecases.integration.movements;

import static com.jbh.account.application.builders.CommandTestBuilder.createBasicAccountCommand;
import static com.jbh.account.application.builders.UseCaseBuilder.DEFAULT_ACCOUNT_TYPE;
import static com.jbh.account.domain.utils.JbhMoneyUtils.withJBHDecimals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.jbh.account.application.builders.UseCaseBuilder;
import com.jbh.account.application.core.dto.AccountDTO;
import com.jbh.account.application.core.dto.AddBasicMovementDTO;
import com.jbh.account.application.core.dto.MonthlyBalanceDTO;
import com.jbh.account.application.core.services.account.AccountService;
import com.jbh.account.application.core.services.monthlybalance.MonthlyBalanceService;
import com.jbh.account.application.core.usecases.AddMovementUseCase;
import com.jbh.account.application.core.usecases.AddTransferJbhAccountsUseCase;
import com.jbh.account.application.core.usecases.CreateAccountUseCase;
import com.jbh.account.application.core.vo.commands.AddMovementCommand;
import com.jbh.account.application.core.vo.commands.AddTransferCommand;
import com.jbh.account.application.movements.ports.output.AccountMovementWriterRepository;
import com.jbh.account.domain.exceptions.AccountBusinessException;
import com.jbh.account.domain.vo.AccountPK;
import com.jbh.account.domain.vo.IncomeCategory;
import com.jbh.account.domain.vo.MovementCategoryDTO;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.MethodOrderer.OrderAnnotation;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

@TestMethodOrder(OrderAnnotation.class)
public class AddTransferAccountsITTest {

  private static final YearMonth createdAccountsPeriod = YearMonth.of(2025, 8);
  static UUID fromUserId = UUID.randomUUID();
  static UUID toUserId = UUID.randomUUID();
  static AccountDTO fromAccount;
  static AccountPK fromAccountPK;
  static AccountDTO toAccount;
  static AccountPK toAccountPK;
  static AddTransferJbhAccountsUseCase transferUseCase;
  @Mock private static AccountMovementWriterRepository accountMovementRepository;
  private static CreateAccountUseCase createAccountUseCase;
  private static AddMovementUseCase addMovementUseCase;
  private static AccountService accountService;
  private static MonthlyBalanceService monthlyBalanceService;

  @BeforeEach
  public void setUp() {
    MockitoAnnotations.openMocks(this);
    transferUseCase = UseCaseBuilder.buildAddTransferUseCase(accountMovementRepository);

    createAccountUseCase = UseCaseBuilder.buildCreateAccountUseCase();

    addMovementUseCase = UseCaseBuilder.buildAddMovementUseCase(accountMovementRepository);

    accountService = UseCaseBuilder.buildAccountService();

    monthlyBalanceService = UseCaseBuilder.buildMonthlyBalanceService();

    UseCaseBuilder.delayTests();
  }

  @Test
  @Order(0)
  void shouldCreateAccounts() throws Exception {

    final String fromAccountName = "From Account";
    final String toAccountName = "To Account";

    fromAccount =
        createAccountUseCase.execute(
            createBasicAccountCommand(fromUserId, fromAccountName, DEFAULT_ACCOUNT_TYPE));
    fromAccountPK = new AccountPK(fromUserId, fromAccount.id());

    toAccount =
        createAccountUseCase.execute(
            createBasicAccountCommand(toUserId, toAccountName, DEFAULT_ACCOUNT_TYPE));
    toAccountPK = new AccountPK(toUserId, toAccount.id());

    final AddMovementCommand fromAccountInitialBalance =
        new AddMovementCommand(
            createdAccountsPeriod.atDay(1),
            withJBHDecimals(new BigDecimal("1000")),
            MovementCategoryDTO.withType(IncomeCategory.INITIAL_BALANCE));

    final AddMovementCommand toAccountInitialBalance =
        new AddMovementCommand(
            createdAccountsPeriod.atDay(2),
            withJBHDecimals(new BigDecimal("2000")),
            MovementCategoryDTO.withType(IncomeCategory.INITIAL_BALANCE));

    // Execute movements concurrently to simulate different users adding movements at the same time
    final ExecutorService executorService = Executors.newFixedThreadPool(2);
    final CountDownLatch latch = new CountDownLatch(2);

    try {
      executorService.submit(
          () -> {
            try {
              final AddBasicMovementDTO result =
                  addMovementUseCase.addMovement(
                      fromUserId, fromAccount.id(), fromAccountInitialBalance);

              assertNotNull(result);

              fromAccount = result.account();

            } catch (final AccountBusinessException e) {
              throw new RuntimeException(e);
            } finally {
              latch.countDown();
            }
          });

      executorService.submit(
          () -> {
            try {
              final AddBasicMovementDTO result =
                  addMovementUseCase.addMovement(toUserId, toAccount.id(), toAccountInitialBalance);
              assertNotNull(result);
              toAccount = result.account();

            } catch (final AccountBusinessException e) {
              throw new RuntimeException(e);
            } finally {
              latch.countDown();
            }
          });

      // Wait for both movements to complete
      latch.await(10, TimeUnit.SECONDS);
    } finally {
      executorService.shutdown();
    }
  }

  @Test
  void shouldAddTransfer() throws AccountBusinessException {
    final LocalDate transferDate = createdAccountsPeriod.atEndOfMonth();
    final var transferAmount = new BigDecimal("100");
    transferUseCase.addTransfer(
        fromAccountPK,
        new AddTransferCommand(toAccountPK, withJBHDecimals(transferAmount), transferDate));

    // From Account Assertions
    final AccountDTO fromAccountUpdated =
        accountService.findAccountOrThrow(fromAccountPK.accountId());
    assertEquals(withJBHDecimals(new BigDecimal("900")), fromAccountUpdated.currentBalance());

    Optional<MonthlyBalanceDTO> updatedMonthlyBalance =
        monthlyBalanceService.findByAccountIdAndPeriod(
            fromAccountUpdated.id(), createdAccountsPeriod);
    assertTrue(updatedMonthlyBalance.isPresent());

    // To Account Assertions

    final AccountDTO toAccountUpdated = accountService.findAccountOrThrow(toAccountPK.accountId());
    assertEquals(
        withJBHDecimals(toAccount.currentBalance().add(transferAmount)),
        toAccountUpdated.currentBalance());

    updatedMonthlyBalance =
        monthlyBalanceService.findByAccountIdAndPeriod(
            toAccountUpdated.id(), createdAccountsPeriod);
    assertTrue(updatedMonthlyBalance.isPresent());
  }
}

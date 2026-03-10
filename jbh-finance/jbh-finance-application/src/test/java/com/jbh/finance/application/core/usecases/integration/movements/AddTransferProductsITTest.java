package com.jbh.finance.application.core.usecases.integration.movements;

import static com.jbh.commons.util.JbhMoneyUtils.withJBHDecimals;
import static com.jbh.finance.application.builders.CommandTestBuilder.createBasicAccountCommand;
import static com.jbh.finance.application.builders.CommandTestBuilder.createLoanCommand;
import static com.jbh.finance.application.builders.UseCaseBuilder.DEFAULT_ACCOUNT_TYPE;
import static com.jbh.finance.application.builders.UseCaseBuilder.delayTests;
import static com.jbh.finance.testfixtures.CategoryFixturesTestApp.INCOME_INITIAL_BALANCE;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.jbh.commons.exception.BusinessException;
import com.jbh.finance.application.builders.UseCaseBuilder;
import com.jbh.finance.application.builders.commands.AddMovementCommandTestBuilder;
import com.jbh.finance.application.feature.monthlybalance.dto.MonthlyBalanceDTO;
import com.jbh.finance.application.feature.monthlybalance.services.MonthlyBalanceLifecycleService;
import com.jbh.finance.application.feature.movement.commands.AddMovementCommand;
import com.jbh.finance.application.feature.movement.commands.AddTransferCommand;
import com.jbh.finance.application.feature.movement.dto.AddMovementResultDTO;
import com.jbh.finance.application.feature.movement.ports.output.MovementWriterRepository;
import com.jbh.finance.application.feature.movement.usecases.AddMovementUseCase;
import com.jbh.finance.application.feature.movement.usecases.AddTransferJbhProductsUseCase;
import com.jbh.finance.application.feature.product.commands.UpdateMetadataProductCommand;
import com.jbh.finance.application.feature.product.dto.ProductDTO;
import com.jbh.finance.application.feature.product.services.ProductLifecycleService;
import com.jbh.finance.application.feature.product.usecases.CreateProductUseCase;
import com.jbh.finance.application.feature.product.usecases.UpdateProductUseCase;
import com.jbh.finance.domain.product.vo.ProductMetadata;
import com.jbh.finance.domain.product.vo.ProductPK;
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
public class AddTransferProductsITTest {

  public static final String PAYOFF_TODAY_INITIAL = "550.00";
  public static final BigDecimal LOAN_TOTAL_AMOUNT_PAID_INITIAL = new BigDecimal("450.00");
  private static final YearMonth createdAccountsPeriod = YearMonth.of(2025, 8);
  private static final ProductMetadata loanMetadata = ProductMetadata.empty();
  static UUID fromUserId = UUID.randomUUID();
  static UUID toUserId = UUID.randomUUID();
  static ProductDTO fromAccount;
  static ProductPK fromAccountPK;
  static ProductDTO toAccount;
  static ProductDTO loanAccount;
  static ProductPK toAccountPK;
  static AddTransferJbhProductsUseCase transferUseCase;
  @Mock private static MovementWriterRepository accountMovementRepository;
  private static CreateProductUseCase createAccountUseCase;
  private static AddMovementUseCase addMovementUseCase;
  private static ProductLifecycleService accountService;
  private static MonthlyBalanceLifecycleService monthlyBalanceService;
  private static UpdateProductUseCase updateProductUseCase;

  @BeforeEach
  public void setUp() {
    MockitoAnnotations.openMocks(this);
    transferUseCase = UseCaseBuilder.buildAddTransferUseCase(accountMovementRepository);

    createAccountUseCase = UseCaseBuilder.buildCreateAccountUseCase();

    addMovementUseCase = UseCaseBuilder.buildAddMovementUseCase(accountMovementRepository);

    accountService = UseCaseBuilder.buildAccountService();

    monthlyBalanceService = UseCaseBuilder.buildMonthlyBalanceService();

    updateProductUseCase = UseCaseBuilder.buildUpdateProductUseCase();

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
    fromAccountPK = new ProductPK(fromUserId, fromAccount.id());

    toAccount =
        createAccountUseCase.execute(
            createBasicAccountCommand(toUserId, toAccountName, DEFAULT_ACCOUNT_TYPE));
    toAccountPK = new ProductPK(toUserId, toAccount.id());

    loanMetadata.findLoanMetadata().putPrincipalAmount(new BigDecimal("1000.00"));
    loanMetadata.findLoanMetadata().putTotalAmountPaid(LOAN_TOTAL_AMOUNT_PAID_INITIAL);
    loanMetadata.findLoanMetadata().putPayoffAmountToday(new BigDecimal(PAYOFF_TODAY_INITIAL));
    loanAccount =
        createAccountUseCase.execute(createLoanCommand(toUserId, "Loan Account", loanMetadata));
    assertNotNull(loanAccount);

    final AddMovementCommand fromAccountInitialBalance =
        AddMovementCommandTestBuilder.withCategory(
            createdAccountsPeriod.atDay(1),
            withJBHDecimals(new BigDecimal("1000")),
            INCOME_INITIAL_BALANCE);

    final AddMovementCommand toAccountInitialBalance =
        AddMovementCommandTestBuilder.withCategory(
            createdAccountsPeriod.atDay(2),
            withJBHDecimals(new BigDecimal("2000")),
            INCOME_INITIAL_BALANCE);

    // Execute movements concurrently to simulate different users adding movements at the same time
    final ExecutorService executorService = Executors.newFixedThreadPool(2);
    final CountDownLatch latch = new CountDownLatch(2);

    try {
      executorService.submit(
          () -> {
            try {
              final AddMovementResultDTO result =
                  addMovementUseCase.addMovement(
                      fromUserId, fromAccount.id(), fromAccountInitialBalance);

              assertNotNull(result);

              fromAccount = result.productDTO();

            } catch (final BusinessException e) {
              throw new RuntimeException(e);
            } finally {
              latch.countDown();
            }
          });

      executorService.submit(
          () -> {
            try {
              final AddMovementResultDTO result =
                  addMovementUseCase.addMovement(toUserId, toAccount.id(), toAccountInitialBalance);
              assertNotNull(result);
              toAccount = result.productDTO();

            } catch (final BusinessException e) {
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
  @Order(1)
  void shouldAddTransfer1() throws BusinessException {
    final LocalDate transferDate = createdAccountsPeriod.atEndOfMonth();
    final var transferAmount = new BigDecimal("100");
    transferUseCase.addTransfer(
        fromAccountPK,
        new AddTransferCommand(toAccountPK, withJBHDecimals(transferAmount), transferDate));

    // From Account Assertions
    final ProductDTO fromAccountUpdated =
        accountService.findOrThrowByIdProductId(fromAccountPK.productId());
    assertEquals(withJBHDecimals(new BigDecimal("900")), fromAccountUpdated.currentBalance());

    Optional<MonthlyBalanceDTO> updatedMonthlyBalance =
        monthlyBalanceService.findByAccountIdAndPeriod(
            fromAccountUpdated.id(), createdAccountsPeriod);
    assertTrue(updatedMonthlyBalance.isPresent());

    // To Account Assertions

    final ProductDTO toAccountUpdated =
        accountService.findOrThrowByIdProductId(toAccountPK.productId());
    assertEquals(
        withJBHDecimals(toAccount.currentBalance().add(transferAmount)),
        toAccountUpdated.currentBalance());

    updatedMonthlyBalance =
        monthlyBalanceService.findByAccountIdAndPeriod(
            toAccountUpdated.id(), createdAccountsPeriod);
    assertTrue(updatedMonthlyBalance.isPresent());
  }

  @Test
  @Order(2)
  void addTransferToLoan() throws BusinessException {
    final ProductPK fromAccountPK = toAccountPK;
    final ProductDTO fromAccountInitial =
        accountService.findOrThrowByIdProductId(fromAccountPK.productId());
    final YearMonth period = YearMonth.now();
    final LocalDate transferDate = period.atDay(1);

    final Optional<MonthlyBalanceDTO> initialMonthlyBalanceFromAccount =
        monthlyBalanceService.findByAccountIdAndPeriod(fromAccountPK.productId(), period);

    final var transferAmount = new BigDecimal("50");

    transferUseCase.addTransfer(
        fromAccountPK,
        new AddTransferCommand(
            new ProductPK(loanAccount.userId(), loanAccount.id()), transferAmount, transferDate));

    delayTests();

    final ProductDTO loanAccountUpdated = accountService.findOrThrowByIdProductId(loanAccount.id());
    assertEquals(
        loanAccountUpdated.metadata().findLoanMetadata().getTotalAmountPaid(),
        withJBHDecimals(LOAN_TOTAL_AMOUNT_PAID_INITIAL.add(transferAmount)));

    final Optional<MonthlyBalanceDTO> monthlyBalanceLoan =
        monthlyBalanceService.findByAccountIdAndPeriod(loanAccountUpdated.id(), period);
    assertTrue(monthlyBalanceLoan.isEmpty());

    final MonthlyBalanceDTO finalFromAccountMB =
        monthlyBalanceService.findByAccountIdAndPeriod(fromAccountPK.productId(), period).get();

    assertEquals(finalFromAccountMB.totalCredits(), withJBHDecimals(transferAmount));
    assertEquals(finalFromAccountMB.closingBalance(), withJBHDecimals(transferAmount.negate()));
    assertEquals(finalFromAccountMB.movementBalance(), withJBHDecimals(transferAmount.negate()));

    final ProductDTO fromProductUpdated =
        accountService.findOrThrowByIdProductId(fromAccountPK.productId());
    assertEquals(
        fromProductUpdated.currentBalance(),
        fromAccountInitial.currentBalance().subtract(transferAmount));
    assertEquals(
        fromProductUpdated.movementBalance(),
        fromAccountInitial.movementBalance().subtract(transferAmount));
  }

  @Test
  @Order(3)
  void updatePayOffTodayForLoan() throws BusinessException {
    final var payoffAmount = new BigDecimal("300");
    final ProductPK loanAccountPK = new ProductPK(loanAccount.userId(), loanAccount.id());
    final ProductMetadata loanMetadata = ProductMetadata.empty();
    loanMetadata.findLoanMetadata().putPayoffAmountToday(payoffAmount);
    loanMetadata.findLoanMetadata().putPrincipalAmount(LOAN_TOTAL_AMOUNT_PAID_INITIAL);
    final UpdateMetadataProductCommand command = new UpdateMetadataProductCommand(loanMetadata);
    updateProductUseCase.replaceMetadata(loanAccountPK, command);

    final ProductDTO loanAccountUpdated = accountService.findOrThrowByIdProductId(loanAccount.id());
    assertEquals(
        loanAccountUpdated.metadata().findLoanMetadata().getPayoffAmountToday(),
        withJBHDecimals(payoffAmount));
  }

  @Test
  @Order(4)
  void payMoreThanLoan() throws BusinessException {
    final ProductPK fromAccountPK = toAccountPK;
    final ProductDTO fromAccountInitial =
        accountService.findOrThrowByIdProductId(fromAccountPK.productId());
    final YearMonth period = YearMonth.now();
    final LocalDate transferDate = period.atDay(1);

    final ProductDTO initialLoanAccount = accountService.findOrThrowByIdProductId(loanAccount.id());

    final var transferAmount =
        initialLoanAccount
            .metadata()
            .findLoanMetadata()
            .getPayoffAmountToday()
            .add(new BigDecimal("50"));

    assertThrows(
        BusinessException.class,
        () ->
            transferUseCase.addTransfer(
                fromAccountPK,
                new AddTransferCommand(
                    new ProductPK(loanAccount.userId(), loanAccount.id()),
                    transferAmount,
                    transferDate)));
  }

  @Test
  @Order(5)
  void payTheExactPendingToPayOff() throws BusinessException {
    final ProductPK fromAccountPK = toAccountPK;
    final ProductDTO fromAccountInitial =
        accountService.findOrThrowByIdProductId(fromAccountPK.productId());
    final YearMonth period = YearMonth.now();
    final LocalDate transferDate = period.atDay(1);

    final ProductDTO initialLoanAccount = accountService.findOrThrowByIdProductId(loanAccount.id());

    final var transferAmount =
        initialLoanAccount.metadata().findLoanMetadata().getPayoffAmountToday();

    transferUseCase.addTransfer(
        fromAccountPK,
        new AddTransferCommand(
            new ProductPK(loanAccount.userId(), loanAccount.id()), transferAmount, transferDate));

    delayTests();

    final ProductDTO loanAccountUpdated = accountService.findOrThrowByIdProductId(loanAccount.id());
    assertEquals(
        loanAccountUpdated.metadata().findLoanMetadata().getTotalAmountPaid(),
        withJBHDecimals((transferAmount)));

    final Optional<MonthlyBalanceDTO> monthlyBalanceLoan =
        monthlyBalanceService.findByAccountIdAndPeriod(loanAccountUpdated.id(), period);
    assertTrue(monthlyBalanceLoan.isEmpty());

    final ProductDTO fromProductUpdated =
        accountService.findOrThrowByIdProductId(fromAccountPK.productId());
    assertEquals(
        fromProductUpdated.currentBalance(),
        fromAccountInitial.currentBalance().subtract(transferAmount));
    assertEquals(
        fromProductUpdated.movementBalance(),
        fromAccountInitial.movementBalance().subtract(transferAmount));
  }
}

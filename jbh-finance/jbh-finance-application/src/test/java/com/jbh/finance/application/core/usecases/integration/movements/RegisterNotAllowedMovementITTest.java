package com.jbh.finance.application.core.usecases.integration.movements;

import static com.jbh.finance.application.builders.commands.AddMovementCommandTestBuilder.createDepositIncome;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.jbh.commons.exception.BusinessException;
import com.jbh.finance.application.builders.CommandTestBuilder;
import com.jbh.finance.application.builders.UseCaseBuilder;
import com.jbh.finance.application.core.ports.output.product.InMemoryProductRepository;
import com.jbh.finance.application.feature.monthlybalance.services.MonthlyBalanceLifecycleService;
import com.jbh.finance.application.feature.movement.ports.output.MovementWriterRepository;
import com.jbh.finance.application.feature.movement.usecases.AddMovementUseCase;
import com.jbh.finance.application.feature.movement.usecases.LiquidateProductUseCase;
import com.jbh.finance.application.feature.product.dto.ProductDTO;
import com.jbh.finance.application.feature.product.usecases.CreateProductUseCase;
import java.math.BigDecimal;
import java.time.YearMonth;
import java.util.UUID;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class RegisterNotAllowedMovementITTest {
  private static final YearMonth period = YearMonth.of(2023, 1);
  private static final InMemoryProductRepository inMemoryAccountRepo =
      UseCaseBuilder.getAccountRepository();
  private static final UUID userId = UUID.randomUUID();
  private static CreateProductUseCase createAccountUseCase;
  private static AddMovementUseCase addMovementUseCase;
  private static MovementWriterRepository accountMovementRepository;
  private static ProductDTO cdtAccount;
  private static ProductDTO internalAccount;
  private static LiquidateProductUseCase liquidateAccountUseCase;
  private MonthlyBalanceLifecycleService monthlyBalanceService;

  @BeforeAll
  static void beforeAll() {
    UseCaseBuilder.getInMemoryMonthlyBalanceRepos().clearStorage();
    UseCaseBuilder.getAccountRepository().clearStorage();
  }

  @BeforeEach
  public void setUp() {

    accountMovementRepository = UseCaseBuilder.getAccountMovementWriterRepository();
    createAccountUseCase = UseCaseBuilder.buildCreateAccountUseCase();
    addMovementUseCase = UseCaseBuilder.buildAddMovementUseCase(accountMovementRepository);
    monthlyBalanceService = UseCaseBuilder.buildMonthlyBalanceService();

    liquidateAccountUseCase =
        UseCaseBuilder.buildLiquidateAccountUseCase(accountMovementRepository);
  }

  @Test
  public void shouldThrowWhenLoanMovement() throws BusinessException {

    final BigDecimal amount = new BigDecimal("100");
    final ProductDTO loanProduct =
        createAccountUseCase.execute(
            CommandTestBuilder.createMockLoanCommand(userId, "Loan Account"));
    assertNotNull(loanProduct);

    final BusinessException error =
        assertThrows(
            BusinessException.class,
            () -> {
              addMovementUseCase.addMovement(
                  userId, loanProduct.id(), createDepositIncome(period.atDay(1), amount));
            });

    assertNotNull(error);
  }

  @Test
  public void shouldThrowWhenRealstateMovement() throws BusinessException {

    final BigDecimal amount = new BigDecimal("100");
    final ProductDTO realEstateAccount =
        createAccountUseCase.execute(CommandTestBuilder.createMockRealStateCommand(userId));
    assertNotNull(realEstateAccount);

    final BusinessException error =
        assertThrows(
            BusinessException.class,
            () -> {
              addMovementUseCase.addMovement(
                  userId, realEstateAccount.id(), createDepositIncome(period.atDay(1), amount));
            });

    assertNotNull(error);
  }
}

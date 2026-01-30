package com.jbh.products.application.core.usecases.integration.movements;

import static com.jbh.products.application.builders.CommandTestBuilder.createDepositIncome;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.jbh.products.application.builders.CommandTestBuilder;
import com.jbh.products.application.builders.UseCaseBuilder;
import com.jbh.products.application.core.dto.ProductDTO;
import com.jbh.products.application.core.ports.output.account.InMemoryAccountRepository;
import com.jbh.products.application.core.services.monthlybalance.MonthlyBalanceService;
import com.jbh.products.application.core.usecases.AddMovementUseCase;
import com.jbh.products.application.core.usecases.CreateProductUseCase;
import com.jbh.products.application.core.usecases.LiquidateAccountUseCase;
import com.jbh.products.application.movements.ports.output.AccountMovementWriterRepository;
import com.jbh.commons.exception.BusinessException;
import java.math.BigDecimal;
import java.time.YearMonth;
import java.util.UUID;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class RegisterNotAllowedMovementITTest {
  private static final YearMonth period = YearMonth.of(2023, 1);
  private static final InMemoryAccountRepository inMemoryAccountRepo =
      UseCaseBuilder.getAccountRepository();
  private static final UUID userId = UUID.randomUUID();
  private static CreateProductUseCase createAccountUseCase;
  private static AddMovementUseCase addMovementUseCase;
  private static AccountMovementWriterRepository accountMovementRepository;
  private static ProductDTO cdtAccount;
  private static ProductDTO internalAccount;
  private static LiquidateAccountUseCase liquidateAccountUseCase;
  private MonthlyBalanceService monthlyBalanceService;

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

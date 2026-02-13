package com.jbh.finance.application.core.usecases.integration.movements;

import static com.jbh.commons.util.JbhMoneyUtils.JBH_ZERO;
import static com.jbh.finance.application.builders.commands.AddMovementCommandTestBuilder.createInitialBalance;
import static com.jbh.finance.application.core.usecases.utils.ProductITUtils.assertAccount;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.jbh.commons.exception.BusinessException;
import com.jbh.finance.application.builders.CommandTestBuilder;
import com.jbh.finance.application.builders.ProductEntityBuilder;
import com.jbh.finance.application.builders.UseCaseBuilder;
import com.jbh.finance.application.core.ports.output.product.InMemoryProductRepository;
import com.jbh.finance.application.feature.monthlybalance.dto.MonthlyBalanceDTO;
import com.jbh.finance.application.feature.monthlybalance.services.MonthlyBalanceLifecycleService;
import com.jbh.finance.application.feature.movement.dto.LiquidationResultDTO;
import com.jbh.finance.application.feature.movement.ports.output.MovementWriterRepository;
import com.jbh.finance.application.feature.movement.usecases.AddMovementUseCase;
import com.jbh.finance.application.feature.movement.usecases.LiquidateProductUseCase;
import com.jbh.finance.application.feature.product.dto.ProductDTO;
import com.jbh.finance.application.feature.product.mappers.ProductMapper;
import com.jbh.finance.application.feature.product.usecases.CreateProductUseCase;
import com.jbh.finance.domain.product.vo.ProductMetadata;
import com.jbh.finance.domain.product.vo.ProductPK;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.MethodOrderer.OrderAnnotation;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.mockito.MockitoAnnotations;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@TestMethodOrder(OrderAnnotation.class)
public class RegisterCDTMovementITTest {

  static final UUID userId = UUID.randomUUID();
  private static final Logger LOG = LoggerFactory.getLogger(RegisterCDTMovementITTest.class);
  private static final String name = "CDT";
  private static final YearMonth period = YearMonth.of(2023, 1);
  private static final InMemoryProductRepository inMemoryAccountRepo =
      UseCaseBuilder.getAccountRepository();
  private static final BigDecimal CDT_INITIAL_BALANCE = new BigDecimal("100.00");
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
    MockitoAnnotations.openMocks(this);

    accountMovementRepository = UseCaseBuilder.getAccountMovementWriterRepository();
    createAccountUseCase = UseCaseBuilder.buildCreateAccountUseCase();
    addMovementUseCase = UseCaseBuilder.buildAddMovementUseCase(accountMovementRepository);
    monthlyBalanceService = UseCaseBuilder.buildMonthlyBalanceService();

    liquidateAccountUseCase =
        UseCaseBuilder.buildLiquidateAccountUseCase(accountMovementRepository);
    UseCaseBuilder.delayTests();
  }

  @Test
  @Order(0)
  void createAccounts() throws BusinessException {
    final LocalDate mvmDate = period.atDay(1);
    final ProductMetadata metadata = ProductMetadata.empty();
    metadata.findCDTMetadata().putMaturityDate(period.plusMonths(1).atDay(1));
    cdtAccount =
        createAccountUseCase.execute(CommandTestBuilder.createCDTCommand(userId, name, metadata));
    assertNotNull(cdtAccount);
    assertNotNull(cdtAccount.id());

    addMovementUseCase.addMovement(
        userId, cdtAccount.id(), createInitialBalance(mvmDate, CDT_INITIAL_BALANCE));

    final ProductDTO updatedAccount = inMemoryAccountRepo.findByProductId(cdtAccount.id()).get();
    assertNotNull(updatedAccount);

    final var expectedAccountBuilder =
        ProductEntityBuilder.withBuilder(
            cdtAccount.id(),
            userId,
            CDT_INITIAL_BALANCE,
            CDT_INITIAL_BALANCE,
            cdtAccount.name(),
            cdtAccount.type());

    final ProductDTO expectedAccount = ProductMapper.toDTO(expectedAccountBuilder.build());

    assertAccount(expectedAccount, updatedAccount);

    // Should not be able to add a new income movement to the productDTO
    assertThrows(
        BusinessException.class,
        () -> {
          addMovementUseCase.addMovement(
              userId, cdtAccount.id(), createInitialBalance(mvmDate, CDT_INITIAL_BALANCE));
        });

    internalAccount =
        createAccountUseCase.execute(CommandTestBuilder.createSavingAccountCommand(userId));
    assertNotNull(internalAccount);

    addMovementUseCase.addMovement(
        userId, internalAccount.id(), createInitialBalance(mvmDate, new BigDecimal("2000")));
  }

  @Test
  @Order(2)
  void withdrawalCDTMovement() throws BusinessException {
    final YearMonth currentPeriod = period.plusMonths(1);
    final LocalDate mvmDate = currentPeriod.atDay(1);

    final BigDecimal gainedInterest = new BigDecimal("25.00");

    final LiquidationResultDTO result =
        liquidateAccountUseCase.liquidateAccount(
            userId,
            cdtAccount.id(),
            CommandTestBuilder.createLiquidateCommandToInternal(
                new ProductPK(userId, internalAccount.id()),
                CDT_INITIAL_BALANCE.add(gainedInterest),
                mvmDate));
    assertNotNull(result);

    UseCaseBuilder.delayTests();

    final Optional<ProductDTO> updatedCDTAccount =
        inMemoryAccountRepo.findByProductId(cdtAccount.id());

    final ProductDTO expectedCDTAccount =
        ProductDTO.defaultBuilder(userId, cdtAccount.id(), cdtAccount.name(), cdtAccount.type())
            .currentBalance(JBH_ZERO)
            .netProfitBalance(gainedInterest)
            .movementBalance(gainedInterest.negate())
            .netGrowthRate(new BigDecimal("25.00"))
            .isActive(false)
            .build();
    assertAccount(expectedCDTAccount, updatedCDTAccount.get());
    assertTrue(
        updatedCDTAccount.get().metadata().findCommonMetadata().isFullyWithdrawn(),
        "Is not Fully withdrawn");
    assertEquals(
        mvmDate,
        updatedCDTAccount.get().metadata().findCommonMetadata().getFullyWithdrawnDate(),
        "There is not fully withdrawn date");
    UseCaseBuilder.delayTests();

    final Optional<MonthlyBalanceDTO> cdtAccountMonthlyBalanceOpt =
        monthlyBalanceService.findByAccountIdAndPeriod(cdtAccount.id(), currentPeriod);
    assertFalse(cdtAccountMonthlyBalanceOpt.isPresent());

    final Optional<ProductDTO> updatedInternalAccount =
        inMemoryAccountRepo.findByProductId(internalAccount.id());
    assertTrue(updatedInternalAccount.isPresent());

    final Optional<MonthlyBalanceDTO> internalAccountMonthlyBalance =
        monthlyBalanceService.findByAccountIdAndPeriod(internalAccount.id(), currentPeriod);
    assertTrue(internalAccountMonthlyBalance.isPresent());
  }
}

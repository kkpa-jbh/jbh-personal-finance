package com.jbh.finance.test.application.feature.movement.usecases.delete;

import static com.jbh.commons.util.JbhMoneyUtils.withJBHDecimals;
import static com.jbh.finance.test.testfixtures.builders.UseCaseFixtureBuilder.delayTests;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.jbh.commons.exception.BusinessException;
import com.jbh.finance.application.feature.monthlybalance.dto.MonthlyBalanceDTO;
import com.jbh.finance.application.feature.monthlybalance.services.MonthlyBalanceLifecycleService;
import com.jbh.finance.application.feature.movement.commands.AddMovementCommand;
import com.jbh.finance.application.feature.movement.dto.AddMovementResultDTO;
import com.jbh.finance.application.feature.movement.dto.MovementDTO;
import com.jbh.finance.application.feature.movement.services.MovementLifecycleService;
import com.jbh.finance.application.feature.movement.usecases.AddMovementUseCase;
import com.jbh.finance.application.feature.movement.usecases.DeleteMovementUseCase;
import com.jbh.finance.application.feature.product.dto.ProductDTO;
import com.jbh.finance.application.feature.product.services.ProductLifecycleService;
import com.jbh.finance.application.feature.product.usecases.CreateProductUseCase;
import com.jbh.finance.test.testfixtures.builders.UseCaseFixtureBuilder;
import com.jbh.finance.test.testfixtures.builders.commands.AddMovementCommandFixtureBuilder;
import com.jbh.finance.test.testfixtures.builders.commands.GeneralCommandFixtureBuilder;
import com.jbh.finance.test.testfixtures.utils.MonthlyBalanceITUtils;
import com.jbh.finance.test.testfixtures.utils.ProductITUtils;
import java.math.BigDecimal;
import java.time.YearMonth;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.MethodOrderer.OrderAnnotation;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@TestMethodOrder(OrderAnnotation.class)
public class DeleteMovementUseCaseTest {

  static final BigDecimal INITIAL_DEPOSIT = new BigDecimal("100.00");
  static final YearMonth INITIAL_DEPOSIT_MONTH = YearMonth.now();
  static final UUID userId = UUID.randomUUID();
  private static ProductDTO productDTO;
  private static MovementDTO expenseToBeReversed;
  private static ProductDTO latestProductBeforeReversion;
  private static MonthlyBalanceDTO latestMonthlyBalanceBeforeRev;
  private static int dayOfMonth = 1;
  private static MovementDTO income100ToReverse;
  private static MovementDTO income45ToReverse;
  private final Logger log = LoggerFactory.getLogger(this.getClass());
  private DeleteMovementUseCase deleteMovementUseCase;
  private CreateProductUseCase createProductUseCase;
  private AddMovementUseCase addMovementUseCase;
  private MonthlyBalanceLifecycleService monthlyBalanceLifecycleSrv;
  private ProductLifecycleService productLifecycleSrv;
  private MovementLifecycleService movementLifecycleSrv;

  @BeforeAll
  static void beforeAll() {
    UseCaseFixtureBuilder.resetState();
  }

  @BeforeEach
  public void setup() {
    deleteMovementUseCase = UseCaseFixtureBuilder.buildDeleteMovementUseCase();
    createProductUseCase = UseCaseFixtureBuilder.buildCreateProductUseCase();
    addMovementUseCase = UseCaseFixtureBuilder.buildAddMovementUseCase();
    monthlyBalanceLifecycleSrv = UseCaseFixtureBuilder.buildMonthlyBalanceService();
    productLifecycleSrv = UseCaseFixtureBuilder.buildProductLifecycleSrv();
    movementLifecycleSrv = UseCaseFixtureBuilder.buildMovementLifeCycleSrv();
  }

  @Test
  @Order(0)
  void createProductBalance100() throws BusinessException {
    productDTO =
        createProductUseCase.execute(
            GeneralCommandFixtureBuilder.createSavingAccountCommand(userId));

    addMovementUseCase.addMovement(
        userId,
        productDTO.id(),
        AddMovementCommandFixtureBuilder.createInitialBalance(
            INITIAL_DEPOSIT_MONTH.atDay(1), INITIAL_DEPOSIT));
  }

  @Test
  @Order(1)
  void addAmount30() throws BusinessException {
    addIncomeToProduct(new BigDecimal("30"), null);
  }

  private MovementDTO addIncomeToProduct(final BigDecimal amount, final BigDecimal balanceSnapshot)
      throws BusinessException {
    final AddMovementResultDTO result =
        addMovementUseCase.addMovement(
            userId,
            productDTO.id(),
            AddMovementCommandFixtureBuilder.createDepositIncomeWithSnapshot(
                INITIAL_DEPOSIT_MONTH.atDay(dayOfMonth++), amount, balanceSnapshot));

    delayTests();

    return result.movement();
  }

  @Test
  @Order(2)
  void createExpense10ToBeReversed() throws BusinessException {

    updateLatestInfoBeforeReversion();

    delayTests();

    final List<MonthlyBalanceDTO> monthlyBalanceBefore =
        monthlyBalanceLifecycleSrv.findAllByAccountIdUntilNow(productDTO.id());

    assertFalse(monthlyBalanceBefore.isEmpty());

    final var personalExpense = new BigDecimal("10.00");

    final AddMovementCommand command =
        AddMovementCommandFixtureBuilder.createPersonalExpense(
            INITIAL_DEPOSIT_MONTH.atDay(2), personalExpense);

    final AddMovementResultDTO resultDTO =
        addMovementUseCase.addMovement(userId, productDTO.id(), command);

    delayTests();

    expenseToBeReversed = resultDTO.movement();
  }

  private void updateLatestInfoBeforeReversion() {
    delayTests();

    latestProductBeforeReversion = productLifecycleSrv.findProductById(productDTO.id()).get();

    final List<MonthlyBalanceDTO> monthlyBalanceBefore =
        monthlyBalanceLifecycleSrv.findAllByAccountIdUntilNow(productDTO.id());

    assertFalse(monthlyBalanceBefore.isEmpty());

    latestMonthlyBalanceBeforeRev =
        MonthlyBalanceITUtils.getBalanceForPeriod(monthlyBalanceBefore, INITIAL_DEPOSIT_MONTH);

    assertNotNull(latestMonthlyBalanceBeforeRev);
    assertNotNull(latestMonthlyBalanceBeforeRev.period());
  }

  @Test
  @Order(3)
  void deleteExpense() throws BusinessException {
    delayTests();

    final var movToReverse = expenseToBeReversed;

    reverseMovement(movToReverse);
  }

  private void reverseMovement(final MovementDTO movToReverse) throws BusinessException {

    deleteMovementUseCase.deleteMovement(userId, productDTO.id(), movToReverse.id().value());

    delayTests();

    final ProductDTO productDTOAfter = productLifecycleSrv.findProductById(productDTO.id()).get();

    ProductITUtils.assertProduct(latestProductBeforeReversion, productDTOAfter);

    final List<MonthlyBalanceDTO> monthlyBalanceAfter =
        monthlyBalanceLifecycleSrv.findAllByAccountIdUntilNow(productDTO.id());

    MonthlyBalanceITUtils.assertMonthlyBalance(
        latestMonthlyBalanceBeforeRev,
        MonthlyBalanceITUtils.getBalanceForPeriod(monthlyBalanceAfter, INITIAL_DEPOSIT_MONTH));

    assertTrue(movementLifecycleSrv.findById(movToReverse.id().value()).isEmpty());
  }

  @Test
  @Order(4)
  void addIncome100ToBeReversed() throws BusinessException {
    updateLatestInfoBeforeReversion();
    income100ToReverse = addIncomeToProduct(new BigDecimal("100"), null);
  }

  @Test
  @Order(5)
  void deleteIncome100() throws BusinessException {
    reverseMovement(income100ToReverse);
  }

  @Test
  @Order(7)
  void addDeposit20Snapshot165() throws BusinessException {
    addIncomeToProduct(new BigDecimal("20"), new BigDecimal("165"));

    final var productDTOAfter = productLifecycleSrv.findProductById(productDTO.id()).get();

    assertNotNull(productDTOAfter);

    final ProductDTO expected =
        ProductDTO.defaultBuilder(userId, productDTO.id(), productDTO.name(), productDTO.type())
            .movementBalance(withJBHDecimals(new BigDecimal(150)))
            .currentBalance(withJBHDecimals(new BigDecimal(165)))
            .netGrowthRate(withJBHDecimals(new BigDecimal(10.71)))
            .netProfitBalance(withJBHDecimals(new BigDecimal(15)))
            .build();

    ProductITUtils.assertProduct(expected, productDTOAfter);
  }

  @Test
  @Order(8)
  void addIncome45() throws BusinessException {
    updateLatestInfoBeforeReversion();
    income45ToReverse = addIncomeToProduct(new BigDecimal("45"), null);
  }

  @Test
  @Order(9)
  void deleteIncome45() throws BusinessException {
    reverseMovement(income45ToReverse);
  }

  @Test
  @Order(20)
  void createExpenseBalanceSnapshot() throws BusinessException {}
}

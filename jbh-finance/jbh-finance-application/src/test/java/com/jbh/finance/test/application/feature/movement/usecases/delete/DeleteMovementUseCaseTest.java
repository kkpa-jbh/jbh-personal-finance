package com.jbh.finance.test.application.feature.movement.usecases.delete;

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
  private static ProductDTO initialProductDTO;
  private static MonthlyBalanceDTO initialMontlyBalance;
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

    delayTests();

    initialProductDTO = productLifecycleSrv.findProductById(productDTO.id()).get();

    final List<MonthlyBalanceDTO> monthlyBalanceBefore =
        monthlyBalanceLifecycleSrv.findAllByAccountIdUntilNow(productDTO.id());

    assertFalse(monthlyBalanceBefore.isEmpty());

    initialMontlyBalance =
        MonthlyBalanceITUtils.getBalanceForPeriod(monthlyBalanceBefore, INITIAL_DEPOSIT_MONTH);

    assertNotNull(initialMontlyBalance);
    assertNotNull(initialMontlyBalance.period());
  }

  @Test
  @Order(1)
  void createExpenseAmount50() throws BusinessException {

    delayTests();

    final List<MonthlyBalanceDTO> monthlyBalanceBefore =
        monthlyBalanceLifecycleSrv.findAllByAccountIdUntilNow(productDTO.id());

    assertFalse(monthlyBalanceBefore.isEmpty());

    final var personalExpense = new BigDecimal("50.00");

    final AddMovementCommand command =
        AddMovementCommandFixtureBuilder.createPersonalExpense(
            INITIAL_DEPOSIT_MONTH.atDay(2), personalExpense);

    final AddMovementResultDTO resultDTO =
        addMovementUseCase.addMovement(userId, productDTO.id(), command);

    delayTests();

    expenseToBeReversed = resultDTO.movement();
  }

  @Test
  @Order(2)
  void deleteExpense() throws BusinessException {
    delayTests();

    final ProductDTO productDTOBefore = productLifecycleSrv.findProductById(productDTO.id()).get();
    final List<MonthlyBalanceDTO> monthlyBalanceBefore =
        monthlyBalanceLifecycleSrv.findAllByAccountIdUntilNow(productDTO.id());
    deleteMovementUseCase.deleteMovement(userId, productDTO.id(), expenseToBeReversed.id().value());

    delayTests();

    // After reversing the movement
    log.info("ProductDTO after reversing the movement: {}", productDTOBefore);

    final ProductDTO productDTOAfter = productLifecycleSrv.findProductById(productDTO.id()).get();

    // ProductITUtils.assertProduct(initialProductDTO, productDTOAfter);

    final List<MonthlyBalanceDTO> monthlyBalanceAfter =
        monthlyBalanceLifecycleSrv.findAllByAccountIdUntilNow(productDTO.id());

    /*
    MonthlyBalanceITUtils.assertMonthlyBalance(
        initialMontlyBalance,
        MonthlyBalanceITUtils.getBalanceForPeriod(monthlyBalanceAfter, INITIAL_DEPOSIT_MONTH));

     */

    assertTrue(movementLifecycleSrv.findById(expenseToBeReversed.id().value()).isEmpty());
  }

  @Test
  @Order(3)
  void addIncome100() throws BusinessException {}

  @Test
  @Order(4)
  void deleteIncome100() throws BusinessException {}

  @Test
  @Order(5)
  void createExpenseBalanceSnapshot() throws BusinessException {}

  @Test
  @Order(6)
  void createDepositBalanceSnapshot() throws BusinessException {}
}

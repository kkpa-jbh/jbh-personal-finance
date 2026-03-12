package com.jbh.finance.test.application.feature.movement.usecases;

import static com.jbh.commons.util.JbhMoneyUtils.JBH_ZERO;
import static com.jbh.commons.util.JbhMoneyUtils.withJBHDecimals;
import static com.jbh.finance.test.testfixtures.builders.CommandTestBuilder.createBasicAccountCommand;
import static com.jbh.finance.test.testfixtures.builders.UseCaseBuilder.DEFAULT_ACCOUNT_NAME;
import static com.jbh.finance.test.testfixtures.builders.UseCaseBuilder.DEFAULT_ACCOUNT_TYPE;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.jbh.commons.exception.BusinessException;
import com.jbh.finance.application.feature.monthlybalance.dto.MonthlyBalanceDTO;
import com.jbh.finance.application.feature.monthlybalance.services.MonthlyBalanceLifecycleService;
import com.jbh.finance.application.feature.monthlybalance.usecases.RegisterMonthlyBalanceUseCase;
import com.jbh.finance.application.feature.movement.commands.AddMovementCommand;
import com.jbh.finance.application.feature.movement.dto.AddMovementResultDTO;
import com.jbh.finance.application.feature.movement.ports.output.MovementWriterRepository;
import com.jbh.finance.application.feature.movement.usecases.AddMovementUseCase;
import com.jbh.finance.application.feature.product.dto.ProductDTO;
import com.jbh.finance.application.feature.product.dto.ProductDTO.ProductDTOBuilder;
import com.jbh.finance.application.feature.product.usecases.CreateProductUseCase;
import com.jbh.finance.domain.product.vo.ProductId;
import com.jbh.finance.domain.product.vo.ProductMetadataKey;
import com.jbh.finance.domain.product.vo.ProductType;
import com.jbh.finance.test.testfixtures.fakes.product.InMemoryProductRepository;
import com.jbh.finance.test.application.feature.monthlybalance.usecases.AddMovementsAfterMonthlyReportedTest;
import com.jbh.finance.test.testfixtures.builders.UseCaseBuilder;
import com.jbh.finance.test.testfixtures.builders.commands.AddMovementCommandTestBuilder;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AddMovementSavingProductTest {

  static final UUID userId = UUID.randomUUID();
  private static final Logger LOG =
      LoggerFactory.getLogger(AddMovementsAfterMonthlyReportedTest.class);

  private static final String PRODUCT_REPORTED_NAME = "NewSavingAccountWithInitialBalance";
  private static final List<BigDecimal> withdrawalMovements =
      List.of(withJBHDecimals(new BigDecimal("5")));
  private static final List<BigDecimal> depositMovements =
      List.of(withJBHDecimals(new BigDecimal("10")));

  static CreateProductUseCase createAccountUseCase;
  static ProductDTO createdAccount;
  static ProductId productId;
  static MonthlyBalanceLifecycleService monthlyBalanceService;

  private static ProductDTO finalExpectedAccountBalance;
  private static MonthlyBalanceDTO officialReportedBalance;
  private static InMemoryProductRepository inMemoryAccountRepo;
  private final BigDecimal INITIAL_BALANCE_AMOUNT = new BigDecimal("1000.00");
  RegisterMonthlyBalanceUseCase useCaseTest;
  LocalDate runningDate = LocalDate.now();
  @Mock private MovementWriterRepository accountMovementRepository;
  private AddMovementUseCase addMovementUseCase;

  private ProductDTOBuilder PRODUCT_DEFAULT_BUILDER =
      ProductDTO.defaultBuilder(userId, productId, DEFAULT_ACCOUNT_NAME, DEFAULT_ACCOUNT_TYPE);

  @BeforeEach
  public void setUp() {
    MockitoAnnotations.openMocks(this);

    monthlyBalanceService = UseCaseBuilder.buildMonthlyBalanceService();
    inMemoryAccountRepo = UseCaseBuilder.getProductRepoInMemory();

    useCaseTest = UseCaseBuilder.buildRegisterMonthlyBalanceUseCase(accountMovementRepository);

    createAccountUseCase = UseCaseBuilder.buildCreateAccountUseCase();

    addMovementUseCase = UseCaseBuilder.buildAddMovementUseCase(accountMovementRepository);

    PRODUCT_DEFAULT_BUILDER =
        ProductDTO.defaultBuilder(userId, productId, DEFAULT_ACCOUNT_NAME, DEFAULT_ACCOUNT_TYPE);
  }

  @Test
  @Order(0)
  void creatingAccountWithInitialBalance() throws BusinessException {
    final Map<ProductMetadataKey, Object> metadata = new HashMap<>();
    metadata.put(ProductMetadataKey.COMMON_INITIAL_BALANCE, INITIAL_BALANCE_AMOUNT);

    assertThrows(
        BusinessException.class,
        () -> {
          createAccountUseCase.execute(
              createBasicAccountCommand(
                  userId, PRODUCT_REPORTED_NAME, ProductType.SAVINGS, metadata));
        });

    createdAccount =
        createAccountUseCase.execute(
            createBasicAccountCommand(userId, PRODUCT_REPORTED_NAME, ProductType.SAVINGS));

    productId = createdAccount.id();
    LOG.info("Account created with id {}", productId);
    assert productId != null;

    assertEquals(JBH_ZERO, createdAccount.currentBalance());
    assertEquals(JBH_ZERO, createdAccount.movementBalance());
  }

  @Test
  @Order(1)
  void addDeposit1MonthAgo() throws BusinessException {
    final var depositAmount = new BigDecimal("100.00");
    final AddMovementCommand command =
        AddMovementCommandTestBuilder.createDepositIncome(
            LocalDate.of(2026, 02, 10), depositAmount);

    final AddMovementResultDTO result = addMovementUseCase.addMovement(userId, productId, command);

    assert result != null;
    final ProductDTO productDTO = result.productDTO();
    assertEquals(JBH_ZERO, productDTO.netGrowthRate());

    final MonthlyBalanceDTO monthlyBalanceFeb = result.monthlyBalance();
    assertEquals(JBH_ZERO, monthlyBalanceFeb.netGrowthRate());

    System.out.println("Product" + result.productDTO());
    System.out.println("Montly" + result.monthlyBalance());
  }
}

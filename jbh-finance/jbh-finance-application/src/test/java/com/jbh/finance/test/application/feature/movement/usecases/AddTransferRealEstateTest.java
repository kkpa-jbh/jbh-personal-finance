package com.jbh.finance.test.application.feature.movement.usecases;

import static org.junit.jupiter.api.Assertions.assertNotNull;

import com.jbh.commons.exception.BusinessException;
import com.jbh.finance.application.feature.monthlybalance.services.MonthlyBalanceLifecycleService;
import com.jbh.finance.application.feature.movement.ports.output.MovementWriterRepository;
import com.jbh.finance.application.feature.movement.usecases.AddMovementUseCase;
import com.jbh.finance.application.feature.movement.usecases.AddTransferJbhProductsUseCase;
import com.jbh.finance.application.feature.product.dto.ProductDTO;
import com.jbh.finance.application.feature.product.services.ProductLifecycleService;
import com.jbh.finance.application.feature.product.usecases.CreateProductUseCase;
import com.jbh.finance.application.feature.product.usecases.UpdateProductUseCase;
import com.jbh.finance.test.testfixtures.builders.CommandTestBuilder;
import com.jbh.finance.test.testfixtures.builders.UseCaseFixtureBuilder;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.MethodOrderer.OrderAnnotation;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

@TestMethodOrder(OrderAnnotation.class)
public class AddTransferRealEstateTest {
  private static final UUID userId = UUID.randomUUID();
  @Mock private static MovementWriterRepository accountMovementRepository;
  private static CreateProductUseCase createAccountUseCase;
  private static AddMovementUseCase addMovementUseCase;
  private static ProductLifecycleService accountService;
  private static MonthlyBalanceLifecycleService monthlyBalanceService;
  private static UpdateProductUseCase updateProductUseCase;
  private static AddTransferJbhProductsUseCase transferUseCase;

  @BeforeEach
  public void setUp() {
    MockitoAnnotations.openMocks(this);
    transferUseCase = UseCaseFixtureBuilder.buildAddTransferUseCase(accountMovementRepository);

    createAccountUseCase = UseCaseFixtureBuilder.buildCreateAccountUseCase();

    addMovementUseCase = UseCaseFixtureBuilder.buildAddMovementUseCase(accountMovementRepository);

    accountService = UseCaseFixtureBuilder.buildProductLifecycleSrv();

    monthlyBalanceService = UseCaseFixtureBuilder.buildMonthlyBalanceService();

    updateProductUseCase = UseCaseFixtureBuilder.buildUpdateProductUseCase();

    UseCaseFixtureBuilder.delayTests();
  }

  @Test
  @Order(0)
  public void shouldCreateRealEstateAccount() throws BusinessException {
    final ProductDTO realEstateAccount =
        createAccountUseCase.execute(CommandTestBuilder.createMockRealStateCommand(userId));
    assertNotNull(realEstateAccount);
  }
}

package com.jbh.finance.application.core.usecases.integration.movements;

import static org.junit.jupiter.api.Assertions.assertNotNull;

import com.jbh.commons.exception.BusinessException;
import com.jbh.finance.application.builders.CommandTestBuilder;
import com.jbh.finance.application.builders.UseCaseBuilder;
import com.jbh.finance.application.feature.monthlybalance.services.MonthlyBalanceLifecycleService;
import com.jbh.finance.application.feature.movement.ports.output.AccountMovementWriterRepository;
import com.jbh.finance.application.feature.movement.usecases.AddMovementUseCase;
import com.jbh.finance.application.feature.movement.usecases.AddTransferJbhAccountsUseCase;
import com.jbh.finance.application.feature.product.dto.ProductDTO;
import com.jbh.finance.application.feature.product.services.ProductLifecycleService;
import com.jbh.finance.application.feature.product.usecases.CreateProductUseCase;
import com.jbh.finance.application.feature.product.usecases.UpdateProductUseCase;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.MethodOrderer.OrderAnnotation;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

@TestMethodOrder(OrderAnnotation.class)
public class AddTransferRealEstateITTest {
  private static final UUID userId = UUID.randomUUID();
  @Mock private static AccountMovementWriterRepository accountMovementRepository;
  private static CreateProductUseCase createAccountUseCase;
  private static AddMovementUseCase addMovementUseCase;
  private static ProductLifecycleService accountService;
  private static MonthlyBalanceLifecycleService monthlyBalanceService;
  private static UpdateProductUseCase updateProductUseCase;
  private static AddTransferJbhAccountsUseCase transferUseCase;

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
  public void shouldCreateRealEstateAccount() throws BusinessException {
    final ProductDTO realEstateAccount =
        createAccountUseCase.execute(CommandTestBuilder.createMockRealStateCommand(userId));
    assertNotNull(realEstateAccount);
  }
}

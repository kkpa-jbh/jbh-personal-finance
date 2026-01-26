package com.jbh.account.application.core.usecases.integration.movements;

import static org.junit.jupiter.api.Assertions.assertNotNull;

import com.jbh.account.application.builders.CommandTestBuilder;
import com.jbh.account.application.builders.UseCaseBuilder;
import com.jbh.account.application.core.dto.ProductDTO;
import com.jbh.account.application.core.services.account.AccountService;
import com.jbh.account.application.core.services.monthlybalance.MonthlyBalanceService;
import com.jbh.account.application.core.usecases.AddMovementUseCase;
import com.jbh.account.application.core.usecases.AddTransferJbhAccountsUseCase;
import com.jbh.account.application.core.usecases.CreateProductUseCase;
import com.jbh.account.application.core.usecases.UpdateProductUseCase;
import com.jbh.account.application.movements.ports.output.AccountMovementWriterRepository;
import com.jbh.commons.exception.BusinessException;
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
  private static AccountService accountService;
  private static MonthlyBalanceService monthlyBalanceService;
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

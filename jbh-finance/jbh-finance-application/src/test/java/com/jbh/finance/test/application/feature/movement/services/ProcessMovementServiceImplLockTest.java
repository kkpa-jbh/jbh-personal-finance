package com.jbh.finance.test.application.feature.movement.services;

import static com.jbh.commons.util.JbhMoneyUtils.JBH_ZERO;
import static com.jbh.finance.domain.movement.vo.MovementType.DEPOSIT;
import static com.jbh.finance.test.testfixtures.CategoryFixturesTestApp.OTHER_INCOME_MOVEMENT;
import static com.jbh.finance.test.testfixtures.builders.commands.AddMovementCommandFixtureBuilder.createMovementWithType;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.jbh.commons.exception.BusinessException;
import com.jbh.finance.application.feature.monthlybalance.services.ProcessMonthlyBalanceService;
import com.jbh.finance.application.feature.movement.commands.AddMovementCommand;
import com.jbh.finance.application.feature.movement.dto.MovementDTO;
import com.jbh.finance.application.feature.movement.services.MovementLifecycleService;
import com.jbh.finance.application.feature.movement.services.ProcessMovementServiceImpl;
import com.jbh.finance.application.feature.product.dto.ProductDTO;
import com.jbh.finance.application.feature.product.mappers.ProductMapper;
import com.jbh.finance.application.feature.product.services.ProductLifecycleService;
import com.jbh.finance.application.shared.exceptions.BusinessApplicationExceptionType;
import com.jbh.finance.domain.product.vo.ProductId;
import com.jbh.finance.domain.product.vo.ProductPK;
import com.jbh.finance.test.testfixtures.CategoryServiceMock;
import com.jbh.finance.test.testfixtures.builders.ProductEntityBuilder;
import com.jbh.finance.test.testfixtures.fakes.acid.RecordingUnitOfWork;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.mockito.InOrder;

@Tag("unit")
class ProcessMovementServiceImplLockTest {

  private final UUID userId = UUID.randomUUID();
  private final ProductId productId = ProductId.generate();
  private final ProductPK productPK = new ProductPK(userId, productId);

  private MovementLifecycleService movementLifecycleService;
  private ProductLifecycleService productLifecycleService;
  private ProcessMonthlyBalanceService processMonthlyBalanceService;
  private RecordingUnitOfWork unitOfWork;
  private ProcessMovementServiceImpl service;

  @BeforeEach
  void setUp() {
    movementLifecycleService = mock(MovementLifecycleService.class);
    productLifecycleService = mock(ProductLifecycleService.class);
    processMonthlyBalanceService = mock(ProcessMonthlyBalanceService.class);
    unitOfWork = new RecordingUnitOfWork();
    service =
        new ProcessMovementServiceImpl(
            movementLifecycleService,
            productLifecycleService,
            processMonthlyBalanceService,
            unitOfWork,
            new CategoryServiceMock());
  }

  @Test
  void shouldLockProductFirstWhenAddingMovement() throws BusinessException {
    // Arrange
    final ProductDTO product =
        ProductMapper.toDTO(
            ProductEntityBuilder.withBasicMovementForExisting(
                productId, userId, JBH_ZERO, JBH_ZERO));
    when(productLifecycleService.syncByMovement(any(), any(), any())).thenReturn(product);
    final AtomicBoolean inTransactionOnLock = new AtomicBoolean();
    final AtomicBoolean inTransactionOnSync = new AtomicBoolean();
    doAnswer(
            invocation -> {
              inTransactionOnLock.set(unitOfWork.isInTransaction());
              return null;
            })
        .when(productLifecycleService)
        .lockForUpdate(productId);
    when(processMonthlyBalanceService.syncForNewMovement(any()))
        .thenAnswer(
            invocation -> {
              inTransactionOnSync.set(unitOfWork.isInTransaction());
              return null;
            });

    // Act
    service.addMovementToProduct(productPK, depositCommand());

    // Assert
    final InOrder inOrder = inOrder(productLifecycleService, processMonthlyBalanceService);
    inOrder.verify(productLifecycleService).lockForUpdate(productId);
    inOrder
        .verify(processMonthlyBalanceService)
        .validateNewMovementForOfficialMonthlyReport(any(MovementDTO.class));
    inOrder.verify(productLifecycleService).syncByMovement(any(), any(), any());
    inOrder.verify(processMonthlyBalanceService).syncForNewMovement(any(MovementDTO.class));
    assertTrue(inTransactionOnLock.get());
    assertTrue(inTransactionOnSync.get());
  }

  @Test
  void shouldPropagateBusinessExceptionWhenValidationFails() throws BusinessException {
    // Arrange
    final BusinessException expected =
        new BusinessException(BusinessApplicationExceptionType.SNAPSHOT_AFTER_OFFICIAL_REPORT);
    doThrow(expected)
        .when(processMonthlyBalanceService)
        .validateNewMovementForOfficialMonthlyReport(any(MovementDTO.class));
    final AddMovementCommand command = depositCommand();

    // Act
    final BusinessException thrown =
        assertThrows(BusinessException.class, () -> service.addMovementToProduct(productPK, command));

    // Assert
    assertSame(expected, thrown);
    verify(processMonthlyBalanceService, never()).syncForNewMovement(any());
  }

  private AddMovementCommand depositCommand() {
    return createMovementWithType(
        LocalDate.now(), new BigDecimal("100.00"), DEPOSIT, OTHER_INCOME_MOVEMENT);
  }
}

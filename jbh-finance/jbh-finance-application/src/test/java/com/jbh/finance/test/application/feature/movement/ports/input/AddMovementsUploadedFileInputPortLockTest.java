package com.jbh.finance.test.application.feature.movement.ports.input;

import static com.jbh.commons.util.JbhMoneyUtils.JBH_ZERO;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.jbh.commons.exception.BusinessException;
import com.jbh.finance.application.feature.monthlybalance.services.MonthlyBalanceSyncForUploadedMovements;
import com.jbh.finance.application.feature.movement.commands.AddMovementUploadedFileCommand;
import com.jbh.finance.application.feature.movement.ports.input.AddMovementsUploadedFileInputPort;
import com.jbh.finance.application.feature.movement.ports.output.MovementWriterRepository;
import com.jbh.finance.application.feature.product.dto.ProductDTO;
import com.jbh.finance.application.feature.product.mappers.ProductMapper;
import com.jbh.finance.application.feature.product.services.ProductLifecycleService;
import com.jbh.finance.domain.movement.vo.MovementType;
import com.jbh.finance.domain.product.vo.ProductId;
import com.jbh.finance.test.testfixtures.CategoryServiceMock;
import com.jbh.finance.test.testfixtures.builders.ProductEntityBuilder;
import com.jbh.finance.test.testfixtures.fakes.acid.RecordingUnitOfWork;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.mockito.InOrder;

@Tag("unit")
class AddMovementsUploadedFileInputPortLockTest {

  @Test
  void shouldLockProductFirstWhenUploadingMovements() throws BusinessException {
    // Arrange
    final UUID userId = UUID.randomUUID();
    final ProductId accountId = ProductId.generate();
    final ProductDTO product =
        ProductMapper.toDTO(
            ProductEntityBuilder.withBasicMovementForExisting(
                accountId, userId, JBH_ZERO, JBH_ZERO));
    final ProductLifecycleService productService = mock(ProductLifecycleService.class);
    when(productService.findOrThrowByUserAndProductId(userId, accountId)).thenReturn(product);
    when(productService.syncByUploadedMovements(any(), any())).thenReturn(product);

    final RecordingUnitOfWork unitOfWork = new RecordingUnitOfWork();
    final AtomicBoolean inTransactionOnSync = new AtomicBoolean();
    final MonthlyBalanceSyncForUploadedMovements balanceSyncer =
        mock(MonthlyBalanceSyncForUploadedMovements.class);
    when(balanceSyncer.syncForUploadedMovementsAsync(eq(accountId), any()))
        .thenAnswer(
            invocation -> {
              inTransactionOnSync.set(unitOfWork.isInTransaction());
              return Collections.emptyList();
            });

    final AddMovementsUploadedFileInputPort inputPort =
        new AddMovementsUploadedFileInputPort(
            productService,
            mock(MovementWriterRepository.class),
            unitOfWork,
            balanceSyncer,
            new CategoryServiceMock());
    final List<AddMovementUploadedFileCommand> commands =
        List.of(deposit("10.00"), deposit("20.00"));

    // Act
    inputPort.uploadMovementsFromFile(userId, accountId, commands);

    // Assert
    final InOrder inOrder = inOrder(productService, balanceSyncer);
    inOrder.verify(productService).lockForUpdate(accountId);
    inOrder.verify(productService).findOrThrowByUserAndProductId(userId, accountId);
    inOrder.verify(productService).syncByUploadedMovements(any(), any());
    inOrder.verify(balanceSyncer).syncForUploadedMovementsAsync(eq(accountId), any());
    assertEquals(1, unitOfWork.transactionCount());
    assertTrue(inTransactionOnSync.get());
  }

  private AddMovementUploadedFileCommand deposit(final String amount) {
    return new AddMovementUploadedFileCommand(
        LocalDate.now(), MovementType.DEPOSIT, new BigDecimal(amount), null);
  }
}

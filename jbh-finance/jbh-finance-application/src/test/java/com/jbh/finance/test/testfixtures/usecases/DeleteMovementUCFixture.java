package com.jbh.finance.test.testfixtures.usecases;

import static com.jbh.finance.test.testfixtures.builders.UseCaseFixtureBuilder.delayTests;

import com.jbh.commons.exception.BusinessException;
import com.jbh.finance.application.feature.movement.ports.input.DeleteMovementInputPort;
import com.jbh.finance.application.feature.movement.services.MovementLifecycleService;
import com.jbh.finance.application.feature.movement.services.ProcessMovementService;
import com.jbh.finance.application.feature.movement.usecases.DeleteMovementUseCase;
import com.jbh.finance.application.feature.product.services.ProductLifecycleService;
import com.jbh.finance.domain.product.vo.ProductId;
import java.util.UUID;

public class DeleteMovementUCFixture implements DeleteMovementUseCase {

  private final MovementLifecycleService movementLifecycleService;
  private final ProductLifecycleService productLifecycleService;
  private final ProcessMovementService processMovementService;

  private final DeleteMovementInputPort deleteMovementInputPort;

  public DeleteMovementUCFixture(
      final MovementLifecycleService movementLifecycleService,
      final ProductLifecycleService productLifecycleService,
      final ProcessMovementService processMovementService) {
    this.movementLifecycleService = movementLifecycleService;
    this.productLifecycleService = productLifecycleService;
    this.processMovementService = processMovementService;

    deleteMovementInputPort =
        new DeleteMovementInputPort(
            movementLifecycleService, productLifecycleService, processMovementService);
  }

  @Override
  public void deleteMovement(final UUID userId, final ProductId productId, final UUID movementId)
      throws BusinessException {
    deleteMovementInputPort.deleteMovement(userId, productId, movementId);
    delayTests();
  }
}

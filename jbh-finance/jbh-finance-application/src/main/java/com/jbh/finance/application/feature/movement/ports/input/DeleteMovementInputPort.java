package com.jbh.finance.application.feature.movement.ports.input;

import com.jbh.commons.exception.BusinessException;
import com.jbh.commons.exception.GenericSpecificationException;
import com.jbh.finance.application.feature.movement.dto.MovementDTO;
import com.jbh.finance.application.feature.movement.services.MovementLifecycleService;
import com.jbh.finance.application.feature.movement.services.ProcessMovementService;
import com.jbh.finance.application.feature.movement.usecases.DeleteMovementUseCase;
import com.jbh.finance.application.feature.product.services.ProductLifecycleService;
import com.jbh.finance.application.shared.exceptions.BusinessApplicationExceptionType;
import com.jbh.finance.domain.product.vo.ProductId;
import com.jbh.finance.domain.product.vo.ProductPK;
import java.util.UUID;

public class DeleteMovementInputPort implements DeleteMovementUseCase {

  private final MovementLifecycleService movementLifecycleService;
  private final ProductLifecycleService productLifecycleService;
  private final ProcessMovementService processMovementService;

  public DeleteMovementInputPort(
      final MovementLifecycleService movementLifecycleService,
      final ProductLifecycleService productLifecycleService,
      final ProcessMovementService processMovementService) {
    this.movementLifecycleService = movementLifecycleService;
    this.productLifecycleService = productLifecycleService;
    this.processMovementService = processMovementService;
  }

  @Override
  public void deleteMovement(final UUID userId, final ProductId productId, final UUID movementId)
      throws BusinessException {

    if (userId == null) {
      throw new GenericSpecificationException("User ID cannot be null");
    }

    productLifecycleService.findOrThrowByUserAndProductId(userId, productId);

    final MovementDTO movement =
        movementLifecycleService
            .findById(movementId)
            .orElseThrow(
                () -> new BusinessException(BusinessApplicationExceptionType.MOVEMENT_NOT_FOUND));

    if (!productId
        .value()
        .equals(movement.productId() != null ? movement.productId().value() : null)) {
      throw new BusinessException(BusinessApplicationExceptionType.MOVEMENT_NOT_FOUND);
    }

    if (!movement.canBeRemoved()) {
      throw new BusinessException(BusinessApplicationExceptionType.MOVEMENT_CANNOT_BE_REMOVED);
    }

    processMovementService.reverseMovementProcessingBalances(
        new ProductPK(userId, productId), movement);
  }
}

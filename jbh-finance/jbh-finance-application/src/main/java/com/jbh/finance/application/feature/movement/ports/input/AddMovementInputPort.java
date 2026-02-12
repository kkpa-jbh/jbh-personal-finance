package com.jbh.finance.application.feature.movement.ports.input;

import com.jbh.commons.exception.BusinessException;
import com.jbh.commons.exception.GenericSpecificationException;
import com.jbh.finance.application.feature.movement.commands.AddMovementCommand;
import com.jbh.finance.application.feature.movement.dto.AddBasicMovementDTO;
import com.jbh.finance.application.feature.movement.services.AccountMovementApplicationService;
import com.jbh.finance.application.feature.movement.usecases.AddMovementUseCase;
import com.jbh.finance.application.feature.product.dto.ProductDTO;
import com.jbh.finance.application.feature.product.services.ProductsService;
import com.jbh.finance.application.shared.exceptions.BusinessApplicationExceptionType;
import com.jbh.finance.domain.product.vo.ProductId;
import com.jbh.finance.domain.product.vo.ProductPK;
import com.jbh.finance.domain.product.vo.ProductType;
import java.util.UUID;

public class AddMovementInputPort implements AddMovementUseCase {

  private final AccountMovementApplicationService accountMovementService;
  private final ProductsService accountService;

  public AddMovementInputPort(
      final AccountMovementApplicationService accountMovementService,
      final ProductsService productsService) {
    this.accountService = productsService;
    this.accountMovementService = accountMovementService;
  }

  @Override
  public AddBasicMovementDTO addMovement(
      final UUID userId, final ProductId productId, final AddMovementCommand movementCommand)
      throws BusinessException {

    if (userId == null) {
      throw new GenericSpecificationException("User ID cannot be null");
    }

    movementCommand.validate();

    final ProductDTO productDTO = accountService.findByUserAndProductId(userId, productId);
    final ProductType productType = productDTO.type();

    if (!productType.addingMovementsProductsAllowed().contains(productType)) {
      throw new BusinessException(BusinessApplicationExceptionType.DISALLOWED_MOVEMENT_FOR_PRODUCT);
    }

    // Sync account balance and persist movement
    final AddBasicMovementDTO addBasicMovementDTO;
    addBasicMovementDTO =
        accountMovementService.addMovementProcessingBalances(
            new ProductPK(userId, productId), movementCommand);

    return addBasicMovementDTO;
  }
}

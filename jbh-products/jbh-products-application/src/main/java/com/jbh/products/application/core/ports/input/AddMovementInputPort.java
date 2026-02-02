package com.jbh.products.application.core.ports.input;

import com.jbh.commons.exception.BusinessException;
import com.jbh.commons.exception.GenericSpecificationException;
import com.jbh.products.application.core.dto.AddBasicMovementDTO;
import com.jbh.products.application.core.dto.ProductDTO;
import com.jbh.products.application.core.exceptions.BusinessApplicationExceptionType;
import com.jbh.products.application.core.services.account.ProductsService;
import com.jbh.products.application.core.services.movements.AccountMovementApplicationService;
import com.jbh.products.application.core.usecases.AddMovementUseCase;
import com.jbh.products.application.core.vo.commands.AddMovementCommand;
import com.jbh.products.domain.vo.ProductId;
import com.jbh.products.domain.vo.ProductPK;
import com.jbh.products.domain.vo.ProductType;
import java.util.UUID;

public class AddMovementInputPort implements AddMovementUseCase {

  private final AccountMovementApplicationService accountMovementService;
  private final ProductsService accountService;

  public AddMovementInputPort(
      final AccountMovementApplicationService accountMovementService,
      final ProductsService accountService) {
    this.accountService = accountService;
    this.accountMovementService = accountMovementService;
  }

  @Override
  public AddBasicMovementDTO addMovement(
      final UUID userId, final ProductId accountId, final AddMovementCommand movementCommand)
      throws BusinessException {

    if (userId == null) {
      throw new GenericSpecificationException("User ID cannot be null");
    }

    movementCommand.validate();

    final ProductDTO productDTO = accountService.findByUserAndProductId(userId, accountId);
    final ProductType productType = productDTO.type();

    if (!productType.addingMovementsProductsAllowed().contains(productType)) {
      throw new BusinessException(BusinessApplicationExceptionType.DISALLOWED_MOVEMENT_FOR_PRODUCT);
    }

    // Sync account balance and persist movement
    final AddBasicMovementDTO addBasicMovementDTO;
    addBasicMovementDTO =
        accountMovementService.addMovementProcessingBalances(
            new ProductPK(userId, accountId), movementCommand);

    return addBasicMovementDTO;
  }
}

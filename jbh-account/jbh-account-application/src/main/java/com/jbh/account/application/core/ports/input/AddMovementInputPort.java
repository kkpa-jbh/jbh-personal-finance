package com.jbh.account.application.core.ports.input;

import com.jbh.account.application.core.dto.AccountDTO;
import com.jbh.account.application.core.dto.AddBasicMovementDTO;
import com.jbh.account.application.core.exceptions.BusinessApplicationExceptionType;
import com.jbh.account.application.core.services.account.AccountService;
import com.jbh.account.application.core.services.movements.AccountMovementApplicationService;
import com.jbh.account.application.core.usecases.AddMovementUseCase;
import com.jbh.account.application.core.vo.commands.AddMovementCommand;
import com.jbh.account.domain.exceptions.AccountBusinessException;
import com.jbh.account.domain.exceptions.GenericSpecificationException;
import com.jbh.account.domain.vo.AccountId;
import com.jbh.account.domain.vo.AccountPK;
import com.jbh.account.domain.vo.ProductType;
import java.util.UUID;

public class AddMovementInputPort implements AddMovementUseCase {

  private final AccountMovementApplicationService accountMovementService;
  private final AccountService accountService;

  public AddMovementInputPort(
      final AccountMovementApplicationService accountMovementService,
      final AccountService accountService) {
    this.accountService = accountService;
    this.accountMovementService = accountMovementService;
  }

  @Override
  public AddBasicMovementDTO addMovement(
      final UUID userId, final AccountId accountId, final AddMovementCommand movementCommand)
      throws AccountBusinessException {

    if (userId == null) {
      throw new GenericSpecificationException("User ID cannot be null");
    }

    movementCommand.validate();

    final AccountDTO productDTO = accountService.findByUserAndAccountId(userId, accountId);
    final ProductType productType = productDTO.type();

    if (!productType.addingMovementsProductsAllowed().contains(productType)) {
      throw new AccountBusinessException(
          BusinessApplicationExceptionType.DISALLOWED_MOVEMENT_FOR_PRODUCT);
    }

    // Sync account balance and persist movement
    final AddBasicMovementDTO addBasicMovementDTO;
    addBasicMovementDTO =
        accountMovementService.addMovementProcessingBalances(
            new AccountPK(userId, accountId), movementCommand);

    return addBasicMovementDTO;
  }
}

package com.jbh.products.application.core.usecases;

import com.jbh.products.application.core.dto.AddBasicMovementDTO;
import com.jbh.products.application.core.vo.commands.AddMovementCommand;
import com.jbh.products.domain.vo.ProductId;
import com.jbh.commons.exception.BusinessException;
import java.util.UUID;

public interface AddMovementUseCase {

  /**
   * Adds a movement to the account. The movement is validated and persisted in the database. The
   * monthly balance is also synced asynchronously. The account balances itself are also synced.
   *
   * @param userId
   * @param accountId
   * @param movementCommand
   * @return
   */
  AddBasicMovementDTO addMovement(
      UUID userId, ProductId accountId, AddMovementCommand movementCommand)
      throws BusinessException;
}

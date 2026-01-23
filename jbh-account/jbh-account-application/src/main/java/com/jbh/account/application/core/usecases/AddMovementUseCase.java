package com.jbh.account.application.core.usecases;

import com.jbh.account.application.core.dto.AddBasicMovementDTO;
import com.jbh.account.application.core.vo.commands.AddMovementCommand;
import com.jbh.account.domain.exceptions.ProductBusinessException;
import com.jbh.account.domain.vo.ProductId;
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
      throws ProductBusinessException;
}

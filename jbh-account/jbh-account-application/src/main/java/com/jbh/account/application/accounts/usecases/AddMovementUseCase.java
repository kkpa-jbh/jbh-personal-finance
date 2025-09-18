package com.jbh.account.application.accounts.usecases;

import com.jbh.account.application.accounts.dto.AddBasicMovementDTO;
import com.jbh.account.application.accounts.vo.AddMovementCommand;
import com.jbh.account.domain.vo.AccountId;
import java.util.UUID;

public interface AddMovementUseCase {

  AddBasicMovementDTO addMovement(
      UUID userId, AccountId accountId, AddMovementCommand movementCommand);
}

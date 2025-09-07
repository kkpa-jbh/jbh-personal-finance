package com.jbh.account.application.accounts.usecases;

import com.jbh.account.application.accounts.dto.AddBasicMovementDTO;
import com.jbh.account.application.accounts.dto.AddMultipleBasicMovementDTO;
import com.jbh.account.application.accounts.vo.AddBasicMovementRequest;
import com.jbh.accounts_mgmt.accounts.AccountId;
import java.util.List;
import java.util.UUID;

public interface RegisterMovementUseCase {

  AddBasicMovementDTO addBasicMovements(UUID userId, AccountId accountId,
      AddBasicMovementRequest basicMovementRequest);

  AddMultipleBasicMovementDTO addBasicMovements(UUID userId, AccountId accountId,
      List<AddBasicMovementRequest> allSimpleMovements);

  //AddCategorizedMovementResponse addCategorizedMovement(
  //      UUID userId,
  //      AccountId accountId,
  //      AddCategorizedMovementRequest request);
}

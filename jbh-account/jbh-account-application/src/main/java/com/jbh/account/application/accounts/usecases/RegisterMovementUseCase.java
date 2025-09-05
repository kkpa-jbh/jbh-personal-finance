package com.jbh.account.application.accounts.usecases;

import com.jbh.account.application.accounts.vo.AddBasicMovementRequest;
import com.jbh.account.application.accounts.vo.AddBasicMovementResponse;
import com.jbh.account.application.accounts.vo.AddMultipleBasicMovementResponse;
import com.jbh.accounts_mgmt.accounts.AccountId;
import java.util.List;
import java.util.UUID;

public interface RegisterMovementUseCase {

  AddBasicMovementResponse addSimpleMovement(UUID userId, AccountId accountId,
      AddBasicMovementRequest basicMovementRequest);

  AddMultipleBasicMovementResponse addSimpleMovement(UUID userId, AccountId accountId,
      List<AddBasicMovementRequest> allSimpleMovements);

  //AddCategorizedMovementResponse addCategorizedMovement(
  //      UUID userId,
  //      AccountId accountId,
  //      AddCategorizedMovementRequest request);
}

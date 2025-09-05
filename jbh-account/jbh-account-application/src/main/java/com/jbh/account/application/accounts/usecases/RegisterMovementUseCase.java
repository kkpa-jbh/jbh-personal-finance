package com.jbh.account.application.accounts.usecases;

import com.jbh.account.application.accounts.vo.AddMovementResponse;
import com.jbh.account.application.accounts.vo.AddSimpleMovementRequest;
import com.jbh.accounts_mgmt.accounts.AccountId;
import java.util.UUID;

public interface RegisterMovementUseCase {

  AddMovementResponse addSimpleMovement(UUID userId, AccountId accountId, AddSimpleMovementRequest requestVO);

  //AddCategorizedMovementResponse addCategorizedMovement(
  //      UUID userId,
  //      AccountId accountId,
  //      AddCategorizedMovementRequest request);
}

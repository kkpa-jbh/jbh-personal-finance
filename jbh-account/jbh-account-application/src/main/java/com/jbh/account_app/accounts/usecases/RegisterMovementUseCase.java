package com.jbh.account_app.accounts.usecases;

import com.jbh.account_app.accounts.vo.AddMovementWithDateAmount;
import com.jbh.accounts_mgmt.accounts.AccountId;
import java.util.UUID;

public interface RegisterMovementUseCase {

  void addSimpleMovement(UUID userId, AccountId accountId, AddMovementWithDateAmount requestVO);
}

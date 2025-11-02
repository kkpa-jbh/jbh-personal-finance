package com.jbh.account.application.core.validation.accounttype;

import com.jbh.account.application.core.dto.AccountDTO;
import com.jbh.account.application.core.dto.MovementDTO;
import com.jbh.account.domain.exceptions.AccountBusinessException;

public class UndefinedAccountMovementValidator implements AccountMovementValidator {

  @Override
  public void validateMovementByAccountType(
      final AccountDTO existingAccount, final MovementDTO movementDTO)
      throws AccountBusinessException {
    // Do nothing
  }
}

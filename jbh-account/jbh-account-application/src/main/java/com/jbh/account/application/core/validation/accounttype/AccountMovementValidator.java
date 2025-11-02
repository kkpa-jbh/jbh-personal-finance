package com.jbh.account.application.core.validation.accounttype;

import com.jbh.account.application.core.dto.AccountDTO;
import com.jbh.account.application.core.dto.MovementDTO;
import com.jbh.account.domain.exceptions.AccountBusinessException;

public interface AccountMovementValidator {

  void validateMovementByAccountType(AccountDTO existingAccount, MovementDTO movementDTO)
      throws AccountBusinessException;
}

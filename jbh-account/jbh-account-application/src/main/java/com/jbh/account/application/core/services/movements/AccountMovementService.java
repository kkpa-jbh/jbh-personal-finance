package com.jbh.account.application.core.services.movements;

import com.jbh.account.application.core.dto.AddBasicMovementDTO;
import com.jbh.account.application.core.dto.MonthlyBalanceDTO;
import com.jbh.account.application.core.vo.commands.AddMovementCommand;
import com.jbh.account.application.exceptions.JbhSpecificationApplication;
import com.jbh.account.domain.vo.AccountPK;

public interface AccountMovementService {

  void addDividendsMovementForNextMonth(AccountPK accountPK, MonthlyBalanceDTO monthlyBalanceDTO)
      throws JbhSpecificationApplication;

  AddBasicMovementDTO addMovement(AccountPK accountPK, AddMovementCommand movementCommand)
      throws JbhSpecificationApplication;
}

package com.jbh.account.application.core.services.movements;

import com.jbh.account.application.core.dto.AddBasicMovementDTO;
import com.jbh.account.application.core.dto.MonthlyBalanceDTO;
import com.jbh.account.application.core.vo.commands.AddMovementCommand;
import com.jbh.account.domain.vo.AccountPK;

public interface AccountMovementService {

  void addDividendsMovement(final AccountPK accountPK, final MonthlyBalanceDTO monthlyBalanceDTO);

  AddBasicMovementDTO addMovement(
      final AccountPK accountPK, final AddMovementCommand movementCommand);
}

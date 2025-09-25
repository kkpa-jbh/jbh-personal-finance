package com.jbh.account.application.core.services.movements;

import com.jbh.account.application.core.dto.MonthlyBalanceDTO;

public interface AccountMovementService {

  void addDividendsMovement(final MonthlyBalanceDTO monthlyBalanceDTO);
}

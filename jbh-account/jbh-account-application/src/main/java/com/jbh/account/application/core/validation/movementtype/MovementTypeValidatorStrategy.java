package com.jbh.account.application.core.validation.movementtype;

import com.jbh.account.application.core.dto.MonthlyBalanceDTO;
import com.jbh.account.domain.exceptions.AccountBusinessException;
import java.math.BigDecimal;

public interface MovementTypeValidatorStrategy {

  void validateMovementAgainstOfficialBalance(
      BigDecimal movementAmount, MonthlyBalanceDTO existingMonthlyBalance)
      throws AccountBusinessException;
}

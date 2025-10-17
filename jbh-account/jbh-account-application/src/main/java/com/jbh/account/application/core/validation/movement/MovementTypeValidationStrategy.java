package com.jbh.account.application.core.validation.movement;

import com.jbh.account.application.core.dto.MonthlyBalanceDTO;
import com.jbh.account.domain.exceptions.AccountBusinessException;
import java.math.BigDecimal;

public interface MovementTypeValidationStrategy {

  void validateMovementAgainstOfficialBalance(
      BigDecimal movementAmount, MonthlyBalanceDTO existingMonthlyBalance)
      throws AccountBusinessException;
}

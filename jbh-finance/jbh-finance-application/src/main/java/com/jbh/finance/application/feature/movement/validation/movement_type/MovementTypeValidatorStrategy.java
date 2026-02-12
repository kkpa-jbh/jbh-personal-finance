package com.jbh.finance.application.feature.movement.validation.movement_type;

import com.jbh.finance.application.feature.monthlybalance.dto.MonthlyBalanceDTO;
import com.jbh.commons.exception.BusinessException;
import java.math.BigDecimal;

public interface MovementTypeValidatorStrategy {

  void validateMovementAgainstOfficialBalance(
      BigDecimal movementAmount, MonthlyBalanceDTO existingMonthlyBalance) throws BusinessException;
}

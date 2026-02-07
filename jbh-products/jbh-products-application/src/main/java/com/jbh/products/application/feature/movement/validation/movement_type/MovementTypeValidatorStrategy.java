package com.jbh.products.application.feature.movement.validation.movement_type;

import com.jbh.products.application.feature.monthlybalance.dto.MonthlyBalanceDTO;
import com.jbh.commons.exception.BusinessException;
import java.math.BigDecimal;

public interface MovementTypeValidatorStrategy {

  void validateMovementAgainstOfficialBalance(
      BigDecimal movementAmount, MonthlyBalanceDTO existingMonthlyBalance) throws BusinessException;
}

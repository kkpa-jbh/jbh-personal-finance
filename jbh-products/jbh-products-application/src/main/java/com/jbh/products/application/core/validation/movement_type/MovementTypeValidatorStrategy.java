package com.jbh.products.application.core.validation.movement_type;

import com.jbh.products.application.core.dto.MonthlyBalanceDTO;
import com.jbh.commons.exception.BusinessException;
import java.math.BigDecimal;

public interface MovementTypeValidatorStrategy {

  void validateMovementAgainstOfficialBalance(
      BigDecimal movementAmount, MonthlyBalanceDTO existingMonthlyBalance) throws BusinessException;
}

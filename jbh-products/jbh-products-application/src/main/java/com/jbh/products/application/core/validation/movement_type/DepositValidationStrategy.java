package com.jbh.products.application.core.validation.movement_type;

import com.jbh.products.application.core.dto.MonthlyBalanceDTO;
import com.jbh.products.application.core.exceptions.BusinessApplicationExceptionType;
import com.jbh.commons.exception.BusinessException;
import java.math.BigDecimal;

public class DepositValidationStrategy implements MovementTypeValidatorStrategy {

  @Override
  public void validateMovementAgainstOfficialBalance(
      final BigDecimal movementAmount, final MonthlyBalanceDTO existingMonthlyBalance)
      throws BusinessException {
    final BigDecimal openingBalance = existingMonthlyBalance.openingBalance();
    final BigDecimal closingBalance = existingMonthlyBalance.closingBalance();
    final BigDecimal movementBalance = existingMonthlyBalance.movementBalance();

    final var futureMovementBalance = openingBalance.add(movementBalance).add(movementAmount);

    if (futureMovementBalance.compareTo(closingBalance) > 0) {
      throw new BusinessException(
          BusinessApplicationExceptionType.DEPOSIT_EXCEEDS_BALANCE, closingBalance);
    }
  }
}

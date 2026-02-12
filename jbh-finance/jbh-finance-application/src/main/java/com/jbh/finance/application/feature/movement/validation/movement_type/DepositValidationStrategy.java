package com.jbh.finance.application.feature.movement.validation.movement_type;

import com.jbh.finance.application.feature.monthlybalance.dto.MonthlyBalanceDTO;
import com.jbh.finance.application.shared.exceptions.BusinessApplicationExceptionType;
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

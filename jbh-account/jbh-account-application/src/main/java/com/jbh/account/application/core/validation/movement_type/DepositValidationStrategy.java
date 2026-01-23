package com.jbh.account.application.core.validation.movement_type;

import com.jbh.account.application.core.dto.MonthlyBalanceDTO;
import com.jbh.account.application.core.exceptions.BusinessApplicationExceptionType;
import com.jbh.account.domain.exceptions.ProductBusinessException;
import java.math.BigDecimal;

public class DepositValidationStrategy implements MovementTypeValidatorStrategy {

  @Override
  public void validateMovementAgainstOfficialBalance(
      final BigDecimal movementAmount, final MonthlyBalanceDTO existingMonthlyBalance)
      throws ProductBusinessException {
    final BigDecimal openingBalance = existingMonthlyBalance.openingBalance();
    final BigDecimal closingBalance = existingMonthlyBalance.closingBalance();
    final BigDecimal movementBalance = existingMonthlyBalance.movementBalance();

    final var futureMovementBalance = openingBalance.add(movementBalance).add(movementAmount);

    if (futureMovementBalance.compareTo(closingBalance) > 0) {
      throw new ProductBusinessException(
          BusinessApplicationExceptionType.DEPOSIT_EXCEEDS_BALANCE, closingBalance);
    }
  }
}

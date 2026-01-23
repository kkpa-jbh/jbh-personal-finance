package com.jbh.account.application.core.validation.movement_type;

import com.jbh.account.application.core.dto.MonthlyBalanceDTO;
import com.jbh.account.application.core.exceptions.BusinessApplicationExceptionType;
import com.jbh.account.domain.exceptions.ProductBusinessException;
import java.math.BigDecimal;

public class WithdrawalValidationStrategy implements MovementTypeValidatorStrategy {

  @Override
  public void validateMovementAgainstOfficialBalance(
      final BigDecimal movementAmount, final MonthlyBalanceDTO existingMonthlyBalance)
      throws ProductBusinessException {
    final BigDecimal openingBalance = existingMonthlyBalance.openingBalance();
    final BigDecimal movementBalance = existingMonthlyBalance.movementBalance();

    final var futureMovementBalance = openingBalance.add(movementBalance).add(movementAmount);
    if (futureMovementBalance.compareTo(BigDecimal.ZERO) < 0) {
      throw new ProductBusinessException(
          BusinessApplicationExceptionType.WITHDRAWAL_EXCEEDS_BALANCE, openingBalance);
    }
  }
}

package com.jbh.account.application.core.validation.movementtype;

import com.jbh.account.application.core.dto.MonthlyBalanceDTO;
import com.jbh.account.domain.exceptions.AccountBusinessException;
import com.jbh.account.domain.exceptions.JbhExceptionMessage;
import java.math.BigDecimal;

public class WithdrawalValidationStrategy implements MovementTypeValidatorStrategy {

  @Override
  public void validateMovementAgainstOfficialBalance(
      final BigDecimal movementAmount, final MonthlyBalanceDTO existingMonthlyBalance)
      throws AccountBusinessException {
    final BigDecimal openingBalance = existingMonthlyBalance.openingBalance();
    final BigDecimal movementBalance = existingMonthlyBalance.movementBalance();

    final var futureMovementBalance = openingBalance.add(movementBalance).add(movementAmount);
    if (futureMovementBalance.compareTo(BigDecimal.ZERO) < 0) {
      throw new AccountBusinessException(
          "The new withdrawal exceeds the monthly balance " + openingBalance,
          new JbhExceptionMessage(
              "The new withdrawal exceeds the monthly balance " + openingBalance,
              "La retirada excede el saldo reportado del mes" + openingBalance));
    }
  }
}

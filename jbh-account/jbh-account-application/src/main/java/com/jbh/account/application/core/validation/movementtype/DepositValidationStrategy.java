package com.jbh.account.application.core.validation.movementtype;

import com.jbh.account.application.core.dto.MonthlyBalanceDTO;
import com.jbh.account.domain.exceptions.AccountBusinessException;
import com.jbh.account.domain.exceptions.JbhExceptionMessage;
import java.math.BigDecimal;

public class DepositValidationStrategy implements MovementTypeValidatorStrategy {

  @Override
  public void validateMovementAgainstOfficialBalance(
      final BigDecimal movementAmount, final MonthlyBalanceDTO existingMonthlyBalance)
      throws AccountBusinessException {
    final BigDecimal openingBalance = existingMonthlyBalance.openingBalance();
    final BigDecimal closingBalance = existingMonthlyBalance.closingBalance();
    final BigDecimal movementBalance = existingMonthlyBalance.movementBalance();

    final var futureMovementBalance = openingBalance.add(movementBalance).add(movementAmount);

    if (futureMovementBalance.compareTo(closingBalance) > 0) {
      throw new AccountBusinessException(
          "The new deposit exceeds the monthly balance " + closingBalance,
          new JbhExceptionMessage(
              "The new deposit exceeds the monthly balance " + closingBalance,
              "El depósito excede el saldo reportado del mes" + closingBalance));
    }
  }
}

package com.jbh.account.application.core.validation.movement;

import com.jbh.account.application.core.dto.MonthlyBalanceDTO;
import com.jbh.account.application.exceptions.JbhExceptionMessage;
import com.jbh.account.application.exceptions.JbhSpecificationApplication;
import java.math.BigDecimal;

public class WithdrawalValidationStrategy implements MovementTypeValidationStrategy {

  @Override
  public void validateMovementAgainstOfficialBalance(
      final BigDecimal movementAmount, final MonthlyBalanceDTO existingMonthlyBalance)
      throws JbhSpecificationApplication {
    final BigDecimal openingBalance = existingMonthlyBalance.openingBalance();
    final BigDecimal movementBalance = existingMonthlyBalance.movementBalance();

    final var futureMovementBalance = openingBalance.add(movementBalance).add(movementAmount);
    if (futureMovementBalance.compareTo(BigDecimal.ZERO) < 0) {
      throw new JbhSpecificationApplication(
          "The new withdrawal exceeds the monthly balance " + openingBalance,
          new JbhExceptionMessage(
              "The new withdrawal exceeds the monthly balance " + openingBalance,
              "La retirada excede el saldo reportado del mes" + openingBalance));
    }
  }
}

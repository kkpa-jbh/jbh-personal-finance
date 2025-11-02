package com.jbh.account.application.core.validation.accounttype;

import com.jbh.account.application.core.dto.AccountDTO;
import com.jbh.account.application.core.dto.MovementDTO;
import com.jbh.account.application.core.exceptions.BusinessApplicationExceptionType;
import com.jbh.account.application.core.services.movements.AccountMovementService;
import com.jbh.account.domain.exceptions.AccountBusinessException;
import com.jbh.account.domain.vo.IncomeCategory;

@SuppressWarnings({
  "PMD.AvoidDeeplyNestedIfStmts",
  "PMD.AvoidLiteralsInIfCondition",
  "PMD.CollapsibleIfStatements"
})
public class CDTAccountMovementValidator implements AccountMovementValidator {
  private final AccountMovementService accountMovementService;

  public CDTAccountMovementValidator(final AccountMovementService accountMovementService) {
    this.accountMovementService = accountMovementService;
  }

  @Override
  public void validateMovementByAccountType(
      final AccountDTO existingAccount, final MovementDTO movementDTO)
      throws AccountBusinessException {
    if (existingAccount.isCDT()) {
      if (movementDTO.movementType().isDeposit()) {
        final int totalCDTMovements =
            accountMovementService.findByAccountId(existingAccount.id()).size();
        if (totalCDTMovements >= 1) {
          throw new AccountBusinessException(
              BusinessApplicationExceptionType.CDT_MOVEMENTS_EXCEEDED);
        }
        if (movementDTO.category().getType() != IncomeCategory.INITIAL_BALANCE) {
          throw new AccountBusinessException(
              BusinessApplicationExceptionType.CDT_WRONG_INCOME_CATEGORY);
        }
      }
    }
  }
}

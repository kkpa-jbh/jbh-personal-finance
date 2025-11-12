package com.jbh.account.application.core.validation.product_type;

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
public class CDTMovementValidator implements ProductMovementValidator {
  private final AccountMovementService accountMovementService;

  public CDTMovementValidator(final AccountMovementService accountMovementService) {
    this.accountMovementService = accountMovementService;
  }

  @Override
  public void validateMovementByProductType(
      final AccountDTO existingProduct, final MovementDTO movementDTO)
      throws AccountBusinessException {
    if (existingProduct.isCDT()) {
      if (movementDTO.movementType().isDeposit()) {
        final int totalCDTMovements =
            accountMovementService.findByAccountId(existingProduct.id()).size();
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

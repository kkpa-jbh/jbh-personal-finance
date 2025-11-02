package com.jbh.account.application.core.validation.accounttype;

import com.jbh.account.application.core.dto.AccountDTO;
import com.jbh.account.application.core.dto.MovementDTO;
import com.jbh.account.application.core.exceptions.BusinessApplicationExceptionType;
import com.jbh.account.domain.exceptions.AccountBusinessException;
import com.jbh.account.domain.vo.ExpenseCategory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class InvestmentAccountMovementValidator implements AccountMovementValidator {

  private static final Logger LOG =
      LoggerFactory.getLogger(InvestmentAccountMovementValidator.class);

  @Override
  public void validateMovementByAccountType(
      final AccountDTO existingAccount, final MovementDTO movementDTO)
      throws AccountBusinessException {

    if (movementDTO.isWithdrawalType() && existingAccount.isFullyWithdrawn()) {

      final var categoryType = movementDTO.category().getType();
      if (categoryType != ExpenseCategory.INVESTMENT_WITHDRAWAL) {
        LOG.error("The Category {} is not valid for Investment accounts", categoryType);
        throw new AccountBusinessException(
            BusinessApplicationExceptionType.INVALID_CATEGORY_INVESTMENT_WITHDRAWAL);
      }
    }
  }
}

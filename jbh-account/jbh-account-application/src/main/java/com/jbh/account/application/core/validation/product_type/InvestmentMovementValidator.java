package com.jbh.account.application.core.validation.product_type;

import com.jbh.account.application.core.dto.MovementDTO;
import com.jbh.account.application.core.dto.ProductDTO;
import com.jbh.account.application.core.exceptions.BusinessApplicationExceptionType;
import com.jbh.account.domain.exceptions.AccountBusinessException;
import com.jbh.account.domain.vo.ExpenseCategory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class InvestmentMovementValidator implements ProductMovementValidator {

  private static final Logger LOG = LoggerFactory.getLogger(InvestmentMovementValidator.class);

  @Override
  public void validateMovementByProductType(
      final ProductDTO existingProduct, final MovementDTO movementDTO)
      throws AccountBusinessException {

    if (movementDTO.isBalanceSnapshot()) {
      return;
    }

    final var categoryType = movementDTO.category().getType();

    if (movementDTO.isWithdrawalType()) {
      if (existingProduct.isFullyWithdrawn()
          && categoryType != ExpenseCategory.INVESTMENT_WITHDRAWAL_TO_CLOSE_IT) {

        LOG.error("The Category {} is not valid for Investment accounts", categoryType);
        throw new AccountBusinessException(
            BusinessApplicationExceptionType.INVALID_CATEGORY_INVESTMENT_WITHDRAWAL);
      } else if (!existingProduct.isFullyWithdrawn()
          && categoryType == ExpenseCategory.INVESTMENT_WITHDRAWAL_TO_CLOSE_IT) {

        throw new AccountBusinessException(
            BusinessApplicationExceptionType.INVALID_LIQUIDATION_AMOUNT);
      }
    }
  }
}

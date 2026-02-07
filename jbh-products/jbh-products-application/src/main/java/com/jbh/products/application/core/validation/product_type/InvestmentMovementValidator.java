package com.jbh.products.application.core.validation.product_type;

import com.jbh.products.application.core.dto.MovementDTO;
import com.jbh.products.application.core.dto.ProductDTO;
import com.jbh.products.application.core.exceptions.BusinessApplicationExceptionType;
import com.jbh.products.domain.movement.vo.ExpenseCategory;
import com.jbh.commons.exception.BusinessException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class InvestmentMovementValidator implements ProductMovementValidator {

  private static final Logger LOG = LoggerFactory.getLogger(InvestmentMovementValidator.class);

  @Override
  public void validateMovementByProductType(
      final ProductDTO existingProduct, final MovementDTO movementDTO) throws BusinessException {

    if (movementDTO.isBalanceSnapshot()) {
      return;
    }

    final var categoryType = movementDTO.category().getType();

    if (movementDTO.isWithdrawalType()) {
      if (existingProduct.isFullyWithdrawn()
          && categoryType != ExpenseCategory.INVESTMENT_WITHDRAWAL_TO_CLOSE_IT) {

        LOG.error("The Category {} is not valid for Investment accounts", categoryType);
        throw new BusinessException(
            BusinessApplicationExceptionType.INVALID_CATEGORY_INVESTMENT_WITHDRAWAL);
      } else if (!existingProduct.isFullyWithdrawn()
          && categoryType == ExpenseCategory.INVESTMENT_WITHDRAWAL_TO_CLOSE_IT) {

        throw new BusinessException(BusinessApplicationExceptionType.INVALID_LIQUIDATION_AMOUNT);
      }
    }
  }
}

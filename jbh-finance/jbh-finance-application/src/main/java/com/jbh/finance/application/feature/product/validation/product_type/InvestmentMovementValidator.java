package com.jbh.finance.application.feature.product.validation.product_type;

import com.jbh.commons.exception.BusinessException;
import com.jbh.finance.application.feature.movement.dto.MovementDTO;
import com.jbh.finance.application.feature.product.dto.ProductDTO;
import com.jbh.finance.application.shared.exceptions.BusinessApplicationExceptionType;
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

    final var categoryType = movementDTO.category();

    if (movementDTO.isWithdrawalType()) {
      if (existingProduct.isFullyWithdrawn() && categoryType.isNotInvestmentToCloseIt()) {

        LOG.error("The Category {} is not valid for Investment products", categoryType);
        throw new BusinessException(
            BusinessApplicationExceptionType.INVALID_CATEGORY_INVESTMENT_WITHDRAWAL);
      } else if (!existingProduct.isFullyWithdrawn() && categoryType.isInvestmentToCloseIt()) {

        throw new BusinessException(BusinessApplicationExceptionType.INVALID_LIQUIDATION_AMOUNT);
      }
    }
  }
}

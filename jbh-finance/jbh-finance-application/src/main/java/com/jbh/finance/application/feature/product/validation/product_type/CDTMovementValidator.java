package com.jbh.finance.application.feature.product.validation.product_type;

import com.jbh.commons.exception.BusinessException;
import com.jbh.finance.application.feature.movement.dto.MovementDTO;
import com.jbh.finance.application.feature.movement.services.MovementService;
import com.jbh.finance.application.feature.product.dto.ProductDTO;
import com.jbh.finance.application.shared.exceptions.BusinessApplicationExceptionType;
import com.jbh.finance.domain.movement.vo.IncomeCategory;

@SuppressWarnings({
  "PMD.AvoidDeeplyNestedIfStmts",
  "PMD.AvoidLiteralsInIfCondition",
  "PMD.CollapsibleIfStatements"
})
public class CDTMovementValidator implements ProductMovementValidator {
  private final MovementService accountMovementService;

  public CDTMovementValidator(final MovementService accountMovementService) {
    this.accountMovementService = accountMovementService;
  }

  @Override
  public void validateMovementByProductType(
      final ProductDTO existingProduct, final MovementDTO movementDTO) throws BusinessException {
    if (existingProduct.isCDT()) {
      if (movementDTO.movementType().isDeposit()) {
        final int totalCDTMovements =
            accountMovementService.findByAccountId(existingProduct.id()).size();
        if (totalCDTMovements >= 1) {
          throw new BusinessException(BusinessApplicationExceptionType.CDT_MOVEMENTS_EXCEEDED);
        }
        if (movementDTO.category().getType() != IncomeCategory.INITIAL_BALANCE) {
          throw new BusinessException(BusinessApplicationExceptionType.CDT_WRONG_INCOME_CATEGORY);
        }
      }
    }
  }
}

package com.jbh.products.application.feature.product.validation.product_type;

import com.jbh.products.application.feature.movement.dto.MovementDTO;
import com.jbh.products.application.feature.product.dto.ProductDTO;
import com.jbh.products.application.shared.exceptions.BusinessApplicationExceptionType;
import com.jbh.products.application.feature.movement.services.AccountMovementService;
import com.jbh.products.domain.movement.vo.IncomeCategory;
import com.jbh.commons.exception.BusinessException;

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

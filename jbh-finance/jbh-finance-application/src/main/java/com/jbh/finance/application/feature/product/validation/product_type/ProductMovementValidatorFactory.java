package com.jbh.finance.application.feature.product.validation.product_type;

import com.jbh.commons.exception.GenericSpecificationException;
import com.jbh.finance.application.feature.movement.services.MovementService;
import com.jbh.finance.domain.product.vo.ProductType;
import java.util.Map;

@SuppressWarnings({"PMD.UnusedPrivateField", "PMD.LawOfDemeter"})
public class ProductMovementValidatorFactory {
  final Map<ProductType, ProductMovementValidator> validators;

  public ProductMovementValidatorFactory(final MovementService accountMovementService) {

    validators =
        Map.of(
            ProductType.CREDIT_CARD, new UndefinedProductMovementValidator(),
            ProductType.SAVINGS, new UndefinedProductMovementValidator(),
            ProductType.LOAN, new LoanMovementValidator(),
            ProductType.INVESTMENT, new InvestmentMovementValidator(),
            ProductType.CDT, new CDTMovementValidator(accountMovementService));
  }

  /**
   * Returns the appropriate validator for the given account type.
   *
   * @param accountType The account type to get validator for
   * @return The validator instance for the account type
   * @throws IllegalArgumentException if no validator is registered for the account type
   */
  public ProductMovementValidator getValidator(final ProductType accountType) {
    final ProductMovementValidator validator = this.validators.get(accountType);

    if (validator == null) {
      throw new GenericSpecificationException(
          "No Product creation validator registered for product type: " + accountType);
    }

    return validator;
  }
}

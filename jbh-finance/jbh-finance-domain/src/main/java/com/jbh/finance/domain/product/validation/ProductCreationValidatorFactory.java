package com.jbh.finance.domain.product.validation;

import com.jbh.finance.domain.product.vo.ProductType;
import java.util.Map;

/**
 * Factory for obtaining the appropriate {@link ProductCreationValidator} for a given {@link
 * ProductType}.
 *
 * <p>This factory implements the Strategy Pattern by providing the correct validator implementation
 * based on the product type, eliminating the need for if/else or switch statements.
 *
 * <p>Usage example:
 *
 * <pre>{@code
 * AccountCreationValidator validator =
 *     AccountValidatorFactory.getValidator(AccountType.CREDIT_CARD);
 * validator.validate(metadata);
 * }</pre>
 */
public final class ProductCreationValidatorFactory {

  private static final Map<ProductType, ProductCreationValidator> VALIDATORS;

  static {
    VALIDATORS =
        Map.ofEntries(
            Map.entry(ProductType.CREDIT_CARD, new CreditCardProductCreationValidator()),
            Map.entry(ProductType.SAVINGS, new SavingsProductCreationValidator()),
            Map.entry(ProductType.INVESTMENT, new InvestmentProductCreationValidator()),
            Map.entry(ProductType.LOAN, new LoanProductCreationValidator()),
            Map.entry(ProductType.CDT, new CdtProductCreationValidator()),
            Map.entry(ProductType.REAL_ESTATE_INVESTMENT, new RealEstateProductCreationValidator()));
  }

  private ProductCreationValidatorFactory() {
    // Utility class - prevent instantiation
    throw new UnsupportedOperationException("This is a utility class and cannot be instantiated");
  }

  /**
   * Returns the appropriate validator for the given product type.
   *
   * @param accountType The product type to get validator for
   * @return The validator instance for the product type
   * @throws IllegalArgumentException if no validator is registered for the product type
   */
  public static ProductCreationValidator getValidator(final ProductType accountType) {
    final ProductCreationValidator validator = VALIDATORS.get(accountType);

    if (validator == null) {
      throw new IllegalArgumentException(
          "No Account creation validator registered for product type: " + accountType);
    }

    return validator;
  }
}

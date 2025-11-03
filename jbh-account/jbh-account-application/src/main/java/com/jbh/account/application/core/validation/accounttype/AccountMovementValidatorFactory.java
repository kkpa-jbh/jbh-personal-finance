package com.jbh.account.application.core.validation.accounttype;

import com.jbh.account.application.core.services.movements.AccountMovementService;
import com.jbh.account.domain.vo.ProductType;
import java.util.Map;

@SuppressWarnings({"PMD.UnusedPrivateField", "PMD.LawOfDemeter"})
public class AccountMovementValidatorFactory {
  final Map<ProductType, AccountMovementValidator> validators;

  public AccountMovementValidatorFactory(final AccountMovementService accountMovementService) {

    validators =
        Map.of(
            ProductType.CREDIT_CARD, new UndefinedAccountMovementValidator(),
            ProductType.SAVINGS, new UndefinedAccountMovementValidator(),
            ProductType.INVESTMENT, new InvestmentAccountMovementValidator(),
            ProductType.CDT, new CDTAccountMovementValidator(accountMovementService));
  }

  /**
   * Returns the appropriate validator for the given account type.
   *
   * @param accountType The account type to get validator for
   * @return The validator instance for the account type
   * @throws IllegalArgumentException if no validator is registered for the account type
   */
  public AccountMovementValidator getValidator(final ProductType accountType) {
    final AccountMovementValidator validator = this.validators.get(accountType);

    if (validator == null) {
      throw new IllegalArgumentException(
          "No Account creation validator registered for account type: " + accountType);
    }

    return validator;
  }
}

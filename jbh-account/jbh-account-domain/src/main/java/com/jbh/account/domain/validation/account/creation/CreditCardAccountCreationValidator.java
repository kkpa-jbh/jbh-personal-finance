package com.jbh.account.domain.validation.account.creation;

import com.jbh.account.domain.entity.AccountMovementDomain;
import com.jbh.account.domain.entity.ProductDomain;
import com.jbh.account.domain.exceptions.AccountBusinessException;
import com.jbh.account.domain.exceptions.BusinessDomainExceptionType;
import com.jbh.account.domain.vo.ProductMetadata;
import com.jbh.account.domain.vo.ProductMetadataKey;
import java.math.BigDecimal;

/**
 * Validator for CREDIT_CARD account type creation.
 *
 * <p>Credit card accounts require:
 *
 * <ul>
 *   <li>CREDIT_LIMIT: BigDecimal greater than zero
 *   <li>PAYMENT_DUE_DAY: Integer between 1 and 31
 * </ul>
 */
@SuppressWarnings("PMD.LawOfDemeter")
public class CreditCardAccountCreationValidator extends BaseAccountCreationValidator
    implements ProductCreationValidator {

  @Override
  public void validateMetadata(final ProductMetadata metadata) throws AccountBusinessException {
    validateCreditLimit(metadata);
    validatePaymentDueDay(metadata);
  }

  private void validateCreditLimit(final ProductMetadata metadata) throws AccountBusinessException {
    if (!metadata.hasKey(ProductMetadataKey.CREDIT_LIMIT)) {
      throw new AccountBusinessException(BusinessDomainExceptionType.MISSING_CREDIT_LIMIT);
    }

    final BigDecimal creditLimit = metadata.getCreditCard().getCreditLimit();

    if (creditLimit.compareTo(BigDecimal.ZERO) <= 0) {
      throw new AccountBusinessException(BusinessDomainExceptionType.INVALID_CREDIT_LIMIT_VALUE);
    }
  }

  protected void validatePaymentDueDay(final ProductMetadata metadata)
      throws AccountBusinessException {
    if (!metadata.hasKey(ProductMetadataKey.PAYMENT_DUE_DAY)) {
      throw new AccountBusinessException(BusinessDomainExceptionType.MISSING_PAYMENT_DUE_DAY);
    }

    final Object paymentDueDay = metadata.getCreditCard().getPaymentDueDay();
    if (!(paymentDueDay instanceof Integer)) {
      throw new AccountBusinessException(BusinessDomainExceptionType.INVALID_PAYMENT_DUE_DAY_TYPE);
    }

    final Integer day = (Integer) paymentDueDay;
    if (day < 1 || day > 31) {
      throw new AccountBusinessException(BusinessDomainExceptionType.INVALID_PAYMENT_DUE_DAY_RANGE);
    }
  }

  @Override
  public void validateInsufficientNetFlow(
      final ProductDomain productDomain, final AccountMovementDomain movement)
      throws AccountBusinessException {
    final var metadata = productDomain.getMetadata();
    validateCreditLimit(metadata);

    final BigDecimal creditLimit = metadata.getCreditCard().getCreditLimit();

    // Check if there is enough credit in the account
    final var movementAmount = movement.getMovementAmount();
    final var futureBalance = productDomain.getCurrentBalance().add(movementAmount);
    if (creditLimit.compareTo(futureBalance.negate()) < 0) {
      throw new AccountBusinessException(BusinessDomainExceptionType.INSUFFICIENT_FUNDS);
    }
  }
}

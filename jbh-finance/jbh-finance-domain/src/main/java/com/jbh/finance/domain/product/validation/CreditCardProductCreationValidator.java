package com.jbh.finance.domain.product.validation;

import com.jbh.commons.exception.BusinessException;
import com.jbh.finance.domain.movement.MovementDomain;
import com.jbh.finance.domain.product.ProductDomain;
import com.jbh.finance.domain.product.vo.ProductMetadata;
import com.jbh.finance.domain.product.vo.ProductMetadataKey;
import com.jbh.finance.domain.shared.exceptions.BusinessDomainExceptionType;
import java.math.BigDecimal;

/**
 * Validator for CREDIT_CARD product type creation.
 *
 * <p>Credit card products require:
 *
 * <ul>
 *   <li>CREDIT_LIMIT: BigDecimal greater than zero
 *   <li>PAYMENT_DUE_DAY: Integer between 1 and 31
 * </ul>
 */
@SuppressWarnings("PMD.LawOfDemeter")
public class CreditCardProductCreationValidator extends BaseProductCreationValidator
    implements ProductCreationValidator {

  @Override
  public void validateMetadata(final ProductMetadata metadata) throws BusinessException {
    validateCreditLimit(metadata);
    validatePaymentDueDay(metadata);
  }

  private void validateCreditLimit(final ProductMetadata metadata) throws BusinessException {
    if (!metadata.hasKey(ProductMetadataKey.CREDIT_LIMIT)) {
      throw new BusinessException(BusinessDomainExceptionType.MISSING_CREDIT_LIMIT);
    }

    final BigDecimal creditLimit = metadata.findCreditCardMetadata().getCreditLimit();

    if (creditLimit.compareTo(BigDecimal.ZERO) <= 0) {
      throw new BusinessException(BusinessDomainExceptionType.INVALID_CREDIT_LIMIT_VALUE);
    }
  }

  protected void validatePaymentDueDay(final ProductMetadata metadata) throws BusinessException {
    if (!metadata.hasKey(ProductMetadataKey.PAYMENT_DUE_DAY)) {
      throw new BusinessException(BusinessDomainExceptionType.MISSING_PAYMENT_DUE_DAY);
    }

    final Object paymentDueDay = metadata.findCreditCardMetadata().getPaymentDueDay();
    if (!(paymentDueDay instanceof Integer)) {
      throw new BusinessException(BusinessDomainExceptionType.INVALID_PAYMENT_DUE_DAY_TYPE);
    }

    final Integer day = (Integer) paymentDueDay;
    if (day < 1 || day > 31) {
      throw new BusinessException(BusinessDomainExceptionType.INVALID_PAYMENT_DUE_DAY_RANGE);
    }
  }

  @Override
  public void validateInsufficientNetFlow(
      final ProductDomain productDomain, final MovementDomain movement) throws BusinessException {
    final var metadata = productDomain.getMetadata();
    validateCreditLimit(metadata);

    final BigDecimal creditLimit = metadata.findCreditCardMetadata().getCreditLimit();

    // Check if there is enough credit in the product
    final var movementAmount = movement.getMovementAmount();
    final var futureBalance = productDomain.getCurrentBalance().add(movementAmount);
    if (creditLimit.compareTo(futureBalance.negate()) < 0) {
      throw new BusinessException(BusinessDomainExceptionType.INSUFFICIENT_FUNDS);
    }
  }
}

package com.jbh.account.domain.validation;

import com.jbh.account.domain.exceptions.AccountBusinessException;
import com.jbh.account.domain.exceptions.BusinessDomainExceptionType;
import com.jbh.account.domain.vo.AccountMetadataKey;
import com.jbh.account.domain.vo.AccountType;
import java.math.BigDecimal;
import java.util.Map;

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
public class CreditCardAccountValidator implements AccountCreationValidator {

  @Override
  public void validate(final Map<String, Object> metadata) throws AccountBusinessException {
    validateCreditLimit(metadata);
    validatePaymentDueDay(metadata);
  }

  private void validateCreditLimit(final Map<String, Object> metadata)
      throws AccountBusinessException {
    final String creditLimitKey = AccountMetadataKey.CREDIT_LIMIT.name();

    if (!metadata.containsKey(creditLimitKey)) {
      throw new AccountBusinessException(BusinessDomainExceptionType.MISSING_CREDIT_LIMIT);
    }

    final Object creditLimit = metadata.get(creditLimitKey);
    if (!(creditLimit instanceof BigDecimal)) {
      throw new AccountBusinessException(BusinessDomainExceptionType.INVALID_CREDIT_LIMIT_TYPE);
    }

    final BigDecimal limit = (BigDecimal) creditLimit;
    if (limit.compareTo(BigDecimal.ZERO) <= 0) {
      throw new AccountBusinessException(BusinessDomainExceptionType.INVALID_CREDIT_LIMIT_VALUE);
    }
  }

  private void validatePaymentDueDay(final Map<String, Object> metadata)
      throws AccountBusinessException {
    final String paymentDueDayKey = AccountMetadataKey.PAYMENT_DUE_DAY.name();

    if (!metadata.containsKey(paymentDueDayKey)) {
      throw new AccountBusinessException(BusinessDomainExceptionType.MISSING_PAYMENT_DUE_DAY);
    }

    final Object paymentDueDay = metadata.get(paymentDueDayKey);
    if (!(paymentDueDay instanceof Integer)) {
      throw new AccountBusinessException(
          BusinessDomainExceptionType.INVALID_PAYMENT_DUE_DAY_TYPE);
    }

    final Integer day = (Integer) paymentDueDay;
    if (day < 1 || day > 31) {
      throw new AccountBusinessException(
          BusinessDomainExceptionType.INVALID_PAYMENT_DUE_DAY_RANGE);
    }
  }

  @Override
  public AccountType supportedAccountType() {
    return AccountType.CREDIT_CARD;
  }
}

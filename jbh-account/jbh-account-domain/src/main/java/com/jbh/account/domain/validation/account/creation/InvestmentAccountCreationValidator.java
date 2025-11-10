package com.jbh.account.domain.validation.account.creation;

import com.jbh.account.domain.entity.AccountMovementDomain;
import com.jbh.account.domain.entity.ProductDomain;
import com.jbh.account.domain.exceptions.AccountBusinessException;
import com.jbh.account.domain.exceptions.BusinessDomainExceptionType;
import com.jbh.account.domain.vo.ProductMetadata;
import com.jbh.account.domain.vo.ProductMetadataKey;

/**
 * Validator for INVESTMENT account type creation.
 *
 * <p>Investment accounts require:
 *
 * <ul>
 *   <li>BROKER_NAME: String identifying the brokerage firm
 * </ul>
 */
public class InvestmentAccountCreationValidator extends BaseAccountCreationValidator
    implements AccountCreationValidator {

  @Override
  public void validateMetadata(final ProductMetadata metadata) throws AccountBusinessException {
    validateBrokerName(metadata);
  }

  private void validateBrokerName(final ProductMetadata metadata)
      throws AccountBusinessException {
    if (!metadata.hasKey(ProductMetadataKey.BROKER_NAME)) {
      throw new AccountBusinessException(BusinessDomainExceptionType.MISSING_BROKER_NAME);
    }

    final Object brokerName = metadata.get(ProductMetadataKey.BROKER_NAME);
    if (brokerName == null || (brokerName instanceof String && ((String) brokerName).isBlank())) {
      throw new AccountBusinessException(BusinessDomainExceptionType.MISSING_BROKER_NAME);
    }

    if (!metadata.hasKey(ProductMetadataKey.COMMISSION_RATE)) {
      throw new AccountBusinessException(BusinessDomainExceptionType.MISSING_COMMISSION_RATE);
    }
  }

  @Override
  public void validateInsufficientNetFlow(
      final ProductDomain account, final AccountMovementDomain movement)
      throws AccountBusinessException {
    // Do Nothing
  }
}

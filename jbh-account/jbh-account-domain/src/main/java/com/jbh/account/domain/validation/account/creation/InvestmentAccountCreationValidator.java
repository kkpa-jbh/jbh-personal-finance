package com.jbh.account.domain.validation.account.creation;

import com.jbh.account.domain.entity.ProductDomain;
import com.jbh.account.domain.entity.ProductMovementDomain;
import com.jbh.account.domain.exceptions.BusinessDomainExceptionType;
import com.jbh.account.domain.vo.ProductMetadata;
import com.jbh.account.domain.vo.ProductMetadataKey;
import com.jbh.commons.exception.BusinessException;

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
    implements ProductCreationValidator {

  @Override
  public void validateMetadata(final ProductMetadata metadata) throws BusinessException {
    validateBrokerName(metadata);
  }

  private void validateBrokerName(final ProductMetadata metadata) throws BusinessException {
    if (!metadata.hasKey(ProductMetadataKey.BROKER_NAME)) {
      throw new BusinessException(BusinessDomainExceptionType.MISSING_BROKER_NAME);
    }

    final String brokerName = metadata.findInvestmentMetadata().getBrokerName();
    if (brokerName == null || brokerName.isBlank()) {
      throw new BusinessException(BusinessDomainExceptionType.MISSING_BROKER_NAME);
    }

    if (!metadata.hasKey(ProductMetadataKey.COMMISSION_RATE)) {
      throw new BusinessException(BusinessDomainExceptionType.MISSING_COMMISSION_RATE);
    }
  }

  @Override
  public void validateInsufficientNetFlow(
      final ProductDomain productDomain, final ProductMovementDomain movement)
      throws BusinessException {
    // Do Nothing
  }
}

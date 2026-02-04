package com.jbh.products.domain.validation.account.creation;

import com.jbh.commons.exception.BusinessException;
import com.jbh.products.domain.entity.MovementDomain;
import com.jbh.products.domain.entity.ProductDomain;
import com.jbh.products.domain.exceptions.BusinessDomainExceptionType;
import com.jbh.products.domain.vo.ProductMetadata;
import com.jbh.products.domain.vo.ProductMetadataKey;

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
      final ProductDomain productDomain, final MovementDomain movement) throws BusinessException {
    // Do Nothing
  }
}

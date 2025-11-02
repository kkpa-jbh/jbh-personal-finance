package com.jbh.account.domain.validation.account.creation;

import com.jbh.account.domain.entity.AccountDomain;
import com.jbh.account.domain.entity.AccountMovementDomain;
import com.jbh.account.domain.exceptions.AccountBusinessException;
import com.jbh.account.domain.exceptions.BusinessDomainExceptionType;
import com.jbh.account.domain.vo.AccountMetadataKey;
import java.util.Map;

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
  public void validateMetadata(final Map<String, Object> metadata) throws AccountBusinessException {
    validateBrokerName(metadata);
  }

  @Override
  public void validateInsufficientNetFlow(
      final AccountDomain account, final AccountMovementDomain movement)
      throws AccountBusinessException {
    defaultValidationInsufficientNetFlow(account, movement);
  }

  private void validateBrokerName(final Map<String, Object> metadata)
      throws AccountBusinessException {
    final String brokerNameKey = AccountMetadataKey.BROKER_NAME.name();

    if (!metadata.containsKey(brokerNameKey)) {
      throw new AccountBusinessException(BusinessDomainExceptionType.MISSING_BROKER_NAME);
    }

    final Object brokerName = metadata.get(brokerNameKey);
    if (brokerName == null || (brokerName instanceof String && ((String) brokerName).isBlank())) {
      throw new AccountBusinessException(BusinessDomainExceptionType.MISSING_BROKER_NAME);
    }

    if (!metadata.containsKey(AccountMetadataKey.COMMISSION_RATE.name())) {
      throw new AccountBusinessException(BusinessDomainExceptionType.MISSING_COMMISSION_RATE);
    }
  }
}

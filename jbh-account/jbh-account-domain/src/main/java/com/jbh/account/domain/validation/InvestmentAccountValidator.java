package com.jbh.account.domain.validation;

import com.jbh.account.domain.exceptions.AccountBusinessException;
import com.jbh.account.domain.exceptions.BusinessDomainExceptionType;
import com.jbh.account.domain.vo.AccountMetadataKey;
import com.jbh.account.domain.vo.AccountType;
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
public class InvestmentAccountValidator implements AccountCreationValidator {

  @Override
  public void validate(final Map<String, Object> metadata) throws AccountBusinessException {
    validateBrokerName(metadata);
  }

  private void validateBrokerName(final Map<String, Object> metadata)
      throws AccountBusinessException {
    final String brokerNameKey = AccountMetadataKey.BROKER_NAME.name();

    if (!metadata.containsKey(brokerNameKey)) {
      throw new AccountBusinessException(BusinessDomainExceptionType.MISSING_BROKER_NAME);
    }

    final Object brokerName = metadata.get(brokerNameKey);
    if (brokerName == null
        || (brokerName instanceof String && ((String) brokerName).isBlank())) {
      throw new AccountBusinessException(BusinessDomainExceptionType.MISSING_BROKER_NAME);
    }
  }

  @Override
  public AccountType supportedAccountType() {
    return AccountType.INVESTMENT;
  }
}

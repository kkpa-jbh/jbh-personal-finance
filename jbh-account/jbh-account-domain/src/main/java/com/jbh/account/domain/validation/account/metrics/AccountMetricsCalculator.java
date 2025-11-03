package com.jbh.account.domain.validation.account.metrics;

import com.jbh.account.domain.entity.AccountDomain;
import com.jbh.account.domain.exceptions.AccountBusinessException;
import java.math.BigDecimal;

public interface AccountMetricsCalculator {

  BigDecimal calculateProfitBalance(AccountDomain accountDomain);

  BigDecimal calculateNetGrowthReate(
      BigDecimal openingBalance, AccountDomain accountDomain, BigDecimal movementAmount)
      throws AccountBusinessException;
}

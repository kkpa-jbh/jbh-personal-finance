package com.jbh.account.domain.entity;

import static com.jbh.account.domain.utils.MoneyUtils.JBH_ZERO;

import com.jbh.account.domain.vo.AccountId;
import java.math.BigDecimal;
import java.time.YearMonth;

public class EntityBuilder {

  public static AccountMonthlyBalanceDomain withInitialDataForNextMonth(
      final AccountId accountId,
      final YearMonth period,
      final BigDecimal closingBalance,
      final boolean gapPeriod) {
    return new AccountMonthlyBalanceDomain(
        null,
        accountId,
        period.getYear(),
        period.getMonthValue(),
        period,
        JBH_ZERO, // movement balance
        JBH_ZERO, // net growth rate
        JBH_ZERO, // total debits
        JBH_ZERO, // total credits
        JBH_ZERO, // opening balance
        closingBalance, // closing balance
        JBH_ZERO, // monthly net profit
        0, // total movements
        gapPeriod,
        false, // official report
        JBH_ZERO,
        null); // monthly profit reported
  }
}

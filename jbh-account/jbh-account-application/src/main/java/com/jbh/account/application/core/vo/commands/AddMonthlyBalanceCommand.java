package com.jbh.account.application.core.vo.commands;

import static com.jbh.account.domain.utils.MoneyUtils.isZero;

import java.math.BigDecimal;
import java.time.YearMonth;

/**
 * The user registers a monthly balance once the month has ended.
 *
 * @param monthlyPeriod The past month to register the balance for.
 * @param closingBalance The final balance for the month.
 * @param monthlyProfitReported The profit reported for the month.Given by the institution account.
 */
public record AddMonthlyBalanceCommand(
    YearMonth monthlyPeriod, BigDecimal closingBalance, BigDecimal monthlyProfitReported)
    implements CommandValidator {

  @Override
  public void validate() {
    if (isZero(closingBalance)) {
      throw new IllegalArgumentException("Closing balance cannot be empty");
    }
    if (monthlyPeriod == null) {
      throw new IllegalArgumentException("Entry date cannot be null");
    }
  }
}

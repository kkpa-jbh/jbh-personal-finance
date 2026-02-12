package com.jbh.finance.application.builders;

import static com.jbh.commons.util.JbhMoneyUtils.withJBHDecimals;

import com.jbh.finance.application.feature.monthlybalance.dto.MonthlyBalanceDTO;
import com.jbh.finance.domain.product.vo.ProductId;
import java.math.BigDecimal;
import java.time.YearMonth;

public class EntityTestBuilder {

  public static MonthlyBalanceDTO.MonthlyBalanceDTOBuilder withClosingBalance(
      final ProductId accountId, final YearMonth period, final BigDecimal closingBalance) {
    return MonthlyBalanceDTO.defaultBuilder()
        .accountId(accountId)
        .period(period)
        .year(period.getYear())
        .month(period.getMonthValue())
        .closingBalance(withJBHDecimals(closingBalance));
  }
}

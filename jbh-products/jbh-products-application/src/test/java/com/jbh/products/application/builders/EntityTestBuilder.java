package com.jbh.products.application.builders;

import static com.jbh.commons.util.JbhMoneyUtils.withJBHDecimals;

import com.jbh.products.application.core.dto.MonthlyBalanceDTO;
import com.jbh.products.domain.product.vo.ProductId;
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

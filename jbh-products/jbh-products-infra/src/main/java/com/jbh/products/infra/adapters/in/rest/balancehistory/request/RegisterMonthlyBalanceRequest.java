package com.jbh.products.infra.adapters.in.rest.balancehistory.request;

import java.math.BigDecimal;
import java.time.YearMonth;

public record RegisterMonthlyBalanceRequest(
    YearMonth monthlyPeriod,
    BigDecimal closingBalance,
    BigDecimal monthlyProfitReported,
    BigDecimal incomeWithholdingTaxAmount) {}

package com.jbh.products.infra.adapters.in.rest.vo.balancehistory;

import java.math.BigDecimal;

public record MonthlyBalanceResponse(
    String period,
    BigDecimal openingBalance,
    BigDecimal closingBalance,
    BigDecimal profit,
    BigDecimal growthRate,
    int movementCount,
    boolean isGapPeriod,
    boolean isOfficialReport,
    String productId,
    String productName,
    String periodLabel,
    boolean isProfitable,
    boolean isLoss) {}

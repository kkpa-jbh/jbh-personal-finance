package com.jbh.products.infra.adapters.in.rest.vo.balancehistory;

import java.math.BigDecimal;

public record BalanceHistorySummary(
    BigDecimal totalBalance,
    BigDecimal periodChange,
    BigDecimal periodChangePercent,
    BigDecimal avgGrowthRate,
    int totalMovements) {}

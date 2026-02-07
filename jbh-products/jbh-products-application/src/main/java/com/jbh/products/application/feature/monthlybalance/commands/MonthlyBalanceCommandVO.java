package com.jbh.products.application.feature.monthlybalance.commands;

import java.math.BigDecimal;

public record MonthlyBalanceCommandVO(
    BigDecimal closingBalance, BigDecimal monthlyProfitReported) {}

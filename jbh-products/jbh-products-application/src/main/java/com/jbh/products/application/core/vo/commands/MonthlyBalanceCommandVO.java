package com.jbh.products.application.core.vo.commands;

import java.math.BigDecimal;

public record MonthlyBalanceCommandVO(
    BigDecimal closingBalance, BigDecimal monthlyProfitReported) {}

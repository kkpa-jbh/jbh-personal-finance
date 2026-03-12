package com.jbh.finance.test.testfixtures.builders.commands;

import java.math.BigDecimal;

public record MonthlyBalanceCommandFixture(
    BigDecimal closingBalance, BigDecimal monthlyProfitReported) {}

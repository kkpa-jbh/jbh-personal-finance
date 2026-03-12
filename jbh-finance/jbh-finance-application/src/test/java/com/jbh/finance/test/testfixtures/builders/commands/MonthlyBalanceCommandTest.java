package com.jbh.finance.test.testfixtures.builders.commands;

import java.math.BigDecimal;

public record MonthlyBalanceCommandTest(
    BigDecimal closingBalance, BigDecimal monthlyProfitReported) {}

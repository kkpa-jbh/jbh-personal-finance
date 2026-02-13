package com.jbh.finance.application.builders.commands;

import java.math.BigDecimal;

public record MonthlyBalanceCommandTest(
    BigDecimal closingBalance, BigDecimal monthlyProfitReported) {}

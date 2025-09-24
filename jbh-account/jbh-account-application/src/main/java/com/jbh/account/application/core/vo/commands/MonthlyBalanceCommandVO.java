package com.jbh.account.application.core.vo.commands;

import java.math.BigDecimal;

public record MonthlyBalanceCommandVO(
    BigDecimal closingBalance, BigDecimal monthlyProfitReported) {}

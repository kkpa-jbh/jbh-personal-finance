package com.jbh.products.infra.adapters.in.rest.vo.balancehistory;

import java.util.List;

public record BalanceHistoryResponse(
    BalanceHistorySummary summary,
    List<MonthlyBalanceResponse> balances) {}

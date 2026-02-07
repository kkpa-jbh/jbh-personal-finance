package com.jbh.products.application.feature.monthlybalance.dto.balancehistory;

import java.util.Collections;
import java.util.List;

public record BalanceHistoryResponse(
    BalanceHistorySummaryResponse summary, List<BalanceHistoryEntryResponse> balances) {

  public static BalanceHistoryResponse empty() {
    return new BalanceHistoryResponse(
        BalanceHistorySummaryResponse.empty(), Collections.emptyList());
  }
}

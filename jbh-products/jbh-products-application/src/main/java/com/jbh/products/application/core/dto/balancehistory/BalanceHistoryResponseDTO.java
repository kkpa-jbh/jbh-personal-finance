package com.jbh.products.application.core.dto.balancehistory;

import java.util.Collections;
import java.util.List;

public record BalanceHistoryResponseDTO(
    BalanceHistorySummary summary, List<MonthlyBalanceResponseDTO> balances) {

  public static BalanceHistoryResponseDTO empty() {
    return new BalanceHistoryResponseDTO(BalanceHistorySummary.empty(), Collections.emptyList());
  }
}

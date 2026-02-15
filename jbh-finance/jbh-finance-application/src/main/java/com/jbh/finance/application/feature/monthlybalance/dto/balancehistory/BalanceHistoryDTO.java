package com.jbh.finance.application.feature.monthlybalance.dto.balancehistory;

import java.util.Collections;
import java.util.List;

public record BalanceHistoryDTO(
    BalanceHistorySummaryDTO summary, List<BalanceHistoryEntryDTO> balances) {

  public static BalanceHistoryDTO empty() {
    return new BalanceHistoryDTO(BalanceHistorySummaryDTO.empty(), Collections.emptyList());
  }
}

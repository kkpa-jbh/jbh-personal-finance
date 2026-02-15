package com.jbh.finance.infra.adapters.in.rest.balancehistory.response;

import com.jbh.finance.application.feature.monthlybalance.dto.balancehistory.BalanceHistoryDTO;
import java.util.List;

/**
 * API response for balance history data.
 *
 * <p>This is the public API contract for balance history information. It maps from internal {@link
 * BalanceHistoryDTO} and is returned by REST controllers.
 */
public record BalanceHistoryResponse(
    BalanceHistorySummaryResponse summary, List<BalanceHistoryEntryResponse> balances) {

  /**
   * Creates a response from the internal DTO.
   *
   * @param dto the internal balance history DTO
   * @return the API response
   */
  public static BalanceHistoryResponse fromDTO(final BalanceHistoryDTO dto) {
    return new BalanceHistoryResponse(
        BalanceHistorySummaryResponse.fromDTO(dto.summary()),
        dto.balances().stream().map(BalanceHistoryEntryResponse::fromDTO).toList());
  }

  /**
   * Creates an empty balance history response.
   *
   * @return empty balance history response
   */
  public static BalanceHistoryResponse empty() {
    return new BalanceHistoryResponse(
        BalanceHistorySummaryResponse.empty(), List.of());
  }
}

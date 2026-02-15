package com.jbh.finance.infra.adapters.in.rest.balancehistory.response;

import com.jbh.finance.application.feature.monthlybalance.dto.balancehistory.BalanceHistoryEntryDTO;
import com.jbh.finance.domain.product.vo.ProductId;
import java.math.BigDecimal;
import java.time.YearMonth;

/**
 * API response for a single balance history entry.
 *
 * <p>Detailed month-by-month breakdown of all balance data.
 *
 * @param period
 * @param openingBalance
 * @param closingBalance
 * @param profit
 * @param growthRate
 * @param movementCount
 * @param isGapPeriod
 * @param isOfficialReport
 * @param productId
 * @param productName
 * @param isProfitable
 * @param isLoss
 * @param totalDebits
 * @param totalCredits
 */
public record BalanceHistoryEntryResponse(
    YearMonth period,
    BigDecimal openingBalance,
    BigDecimal closingBalance,
    BigDecimal profit,
    BigDecimal growthRate,
    int movementCount,
    boolean isGapPeriod,
    boolean isOfficialReport,
    ProductId productId,
    String productName,
    boolean isProfitable,
    boolean isLoss,
    BigDecimal totalDebits,
    BigDecimal totalCredits) {

  /**
   * Creates a response from the internal DTO.
   *
   * @param dto the internal balance history entry DTO
   * @return the API response
   */
  public static BalanceHistoryEntryResponse fromDTO(final BalanceHistoryEntryDTO dto) {
    return new BalanceHistoryEntryResponse(
        dto.period(),
        dto.openingBalance(),
        dto.closingBalance(),
        dto.profit(),
        dto.growthRate(),
        dto.movementCount(),
        dto.isGapPeriod(),
        dto.isOfficialReport(),
        dto.productId(),
        dto.productName(),
        dto.isProfitable(),
        dto.isLoss(),
        dto.totalDebits(),
        dto.totalCredits());
  }
}

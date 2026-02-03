package com.jbh.products.application.core.ports.output.monthlybalance;

import com.jbh.products.application.core.dto.MonthlyBalanceDTO;
import com.jbh.products.domain.vo.ProductId;
import com.jbh.products.domain.vo.ProductPK;
import java.math.BigDecimal;
import java.time.YearMonth;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public interface AccountMonthlyBalanceQueryRepo {

  Optional<MonthlyBalanceDTO> findByAccountIdYearAndMonth(
      ProductId accountId, Integer balanceYear, Integer balanceMonth);

  Optional<MonthlyBalanceDTO> findByAccountIdAndPeriod(ProductId accountId, YearMonth period);

  Optional<MonthlyBalanceDTO> findLastOfficialReport(ProductId accountId);

  BigDecimal sumNetProfitOfficialReported(ProductId accountId);

  List<MonthlyBalanceDTO> findNextBalancesFromPeriodInclusive(
      ProductId accountId, YearMonth currentPeriod);

  List<MonthlyBalanceDTO> findByAccountAndPeriods(
      ProductPK accountPK, YearMonth startPeriod, YearMonth endPeriod);

  List<MonthlyBalanceDTO> findAllByAccountIdUntilNow(ProductId accountId);

  /**
   * Finds monthly balances for multiple products within a date range in a single query. Groups the
   * results by ProductId for efficient lookup.
   *
   * @param productIds the list of product IDs to query
   * @param startPeriod the start period (inclusive)
   * @param endPeriod the end period (inclusive)
   * @return a map where the key is the ProductId and the value is the list of monthly balances
   */
  Map<ProductId, List<MonthlyBalanceDTO>> findByProductIdsAndPeriods(
      List<ProductId> productIds, YearMonth startPeriod, YearMonth endPeriod);
}

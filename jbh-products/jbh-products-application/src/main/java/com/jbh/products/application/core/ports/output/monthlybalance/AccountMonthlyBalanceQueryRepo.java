package com.jbh.products.application.core.ports.output.monthlybalance;

import com.jbh.products.application.core.dto.MonthlyBalanceDTO;
import com.jbh.products.domain.shared.vo.PeriodRange;
import com.jbh.products.domain.product.vo.ProductId;
import com.jbh.products.domain.product.vo.ProductPK;
import java.math.BigDecimal;
import java.time.YearMonth;
import java.util.List;
import java.util.Optional;

public interface AccountMonthlyBalanceQueryRepo {

  List<MonthlyBalanceDTO> findByAccountAndPeriods(
      ProductPK accountPK, YearMonth startPeriod, YearMonth endPeriod);

  Optional<MonthlyBalanceDTO> findByAccountIdYearAndMonth(
      ProductId accountId, Integer balanceYear, Integer balanceMonth);

  Optional<MonthlyBalanceDTO> findByAccountIdAndPeriod(ProductId accountId, YearMonth period);

  Optional<MonthlyBalanceDTO> findLastOfficialReport(ProductId accountId);

  BigDecimal sumNetProfitOfficialReported(ProductId accountId);

  List<MonthlyBalanceDTO> findNextBalancesFromPeriodInclusive(
      ProductId accountId, YearMonth currentPeriod);

  List<MonthlyBalanceDTO> findAllByAccountIdUntilNow(ProductId accountId);

  /**
   * Finds monthly balances for multiple products within a date range in a single query. Groups the
   * results by ProductId for efficient lookup.
   *
   * @param productIds the list of product IDs to query
   * @param periodRange VO that contains period range and if end period is exclusive/inclusive
   */
  List<MonthlyBalanceDTO> findByProductIdsAndPeriods(
      List<ProductId> productIds, PeriodRange periodRange);
}

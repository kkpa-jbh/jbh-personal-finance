package com.jbh.finance.application.feature.monthlybalance.ports.input;

import static com.jbh.commons.util.JbhMoneyUtils.JBH_ZERO;
import static com.jbh.finance.application.shared.exceptions.BusinessApplicationExceptionType.INVALID_RANGE_DATES_FOR_MONTHLY_BALANCES;

import com.jbh.commons.exception.BusinessException;
import com.jbh.commons.util.JbhMoneyUtils;
import com.jbh.finance.application.feature.monthlybalance.dto.MonthlyBalanceDTO;
import com.jbh.finance.application.feature.monthlybalance.dto.balancehistory.BalanceHistoryEntryResponse;
import com.jbh.finance.application.feature.monthlybalance.dto.balancehistory.BalanceHistoryResponse;
import com.jbh.finance.application.feature.monthlybalance.dto.balancehistory.BalanceHistorySummaryResponse;
import com.jbh.finance.application.feature.monthlybalance.services.MonthlyBalanceLifecycleService;
import com.jbh.finance.application.feature.monthlybalance.usecases.FindMonthlyBalanceUseCase;
import com.jbh.finance.application.feature.product.dto.ProductDTO;
import com.jbh.finance.application.feature.product.services.ProductLifecycleService;
import com.jbh.finance.domain.product.vo.ProductId;
import com.jbh.finance.domain.product.vo.ProductPK;
import com.jbh.finance.domain.shared.vo.PeriodRange;
import java.math.BigDecimal;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

public class FindMonthlyBalanceInputPort implements FindMonthlyBalanceUseCase {

  private final MonthlyBalanceLifecycleService monthlyBalanceService;
  private final ProductLifecycleService productService;

  public FindMonthlyBalanceInputPort(
      final MonthlyBalanceLifecycleService monthlyBalanceService,
      final ProductLifecycleService productService) {
    this.monthlyBalanceService = monthlyBalanceService;
    this.productService = productService;
  }

  @Override
  public List<MonthlyBalanceDTO> findMonthlyBalancesByProduct(
      final ProductPK productPK, final YearMonth startPeriod, final YearMonth endPeriod)
      throws BusinessException {

    validatePeriodRange(startPeriod, endPeriod);
    productService.findByUserAndProductId(productPK.userId(), productPK.accountId());

    return monthlyBalanceService.findByAccountAndPeriods(productPK, startPeriod, endPeriod);
  }

  @Override
  public BalanceHistoryResponse findBalanceHistoryByProduct(
      final ProductPK accountPK,
      final YearMonth startPeriod,
      final YearMonth endPeriod,
      final YearMonth today)
      throws BusinessException {

    validatePeriodRange(startPeriod, endPeriod);
    final ProductDTO product =
        productService.findByUserAndProductId(accountPK.userId(), accountPK.accountId());

    return findBalanceHistory(startPeriod, endPeriod, today, Collections.singletonList(product));
  }

  private BalanceHistoryResponse findBalanceHistory(
      final YearMonth startPeriod,
      final YearMonth inputEndPeriod,
      final YearMonth today,
      final List<ProductDTO> allProducts) {
    if (allProducts.isEmpty()) {
      return BalanceHistoryResponse.empty();
    }

    final Map<ProductId, ProductDTO> productsMap =
        allProducts.stream().collect(Collectors.toMap(ProductDTO::id, productDTO -> productDTO));
    final List<ProductId> productIds = allProducts.stream().map(ProductDTO::id).toList();

    final PeriodRange periodRange = PeriodRange.autoExclusive(startPeriod, inputEndPeriod, today);

    final List<MonthlyBalanceDTO> allMonthlyBalances =
        monthlyBalanceService.findByProductIdsAndPeriods(productIds, periodRange);

    if (allMonthlyBalances.isEmpty()) {
      return BalanceHistoryResponse.empty();
    }
    // Logic by transforming the fetched data.

    BigDecimal totalBalanceSummary = JBH_ZERO;

    BigDecimal firstOpeningBalances = JBH_ZERO;
    BigDecimal lastClosingBalances = JBH_ZERO;
    BigDecimal weightedGrowthSum = JBH_ZERO;
    BigDecimal totalWeightForAvg = JBH_ZERO;
    int totalMovementsSummary = 0;

    final List<BalanceHistoryEntryResponse> balancesResponse = new ArrayList<>();

    for (final MonthlyBalanceDTO monthlyBalance : allMonthlyBalances) {

      totalMovementsSummary += monthlyBalance.totalMovements();

      final BigDecimal closingBalance = monthlyBalance.closingBalance();
      final BigDecimal netGrowthRate = monthlyBalance.netGrowthRate();
      if (netGrowthRate != null && closingBalance != null) {
        weightedGrowthSum = weightedGrowthSum.add(netGrowthRate.multiply(closingBalance));
        totalWeightForAvg = totalWeightForAvg.add(closingBalance);
      }

      final YearMonth currentPeriod = monthlyBalance.period();
      if (currentPeriod.equals(periodRange.getStartPeriod())) {
        firstOpeningBalances = firstOpeningBalances.add(monthlyBalance.openingBalance());
      }
      if (currentPeriod.equals(periodRange.getEndPeriodExclusive())) {
        lastClosingBalances = lastClosingBalances.add(monthlyBalance.closingBalance());
        totalBalanceSummary = totalBalanceSummary.add(monthlyBalance.closingBalance());
      }

      balancesResponse.add(
          BalanceHistoryEntryResponse.fromDTO(
              monthlyBalance, productsMap.get(monthlyBalance.accountId())));
    }

    final BigDecimal avgGrowthRateSummary =
        JbhMoneyUtils.isZero(totalWeightForAvg)
            ? JBH_ZERO
            : JbhMoneyUtils.divide(weightedGrowthSum, totalWeightForAvg);

    // This is how much my balance grew (or shrank) during this period
    final BigDecimal periodChange = lastClosingBalances.subtract(firstOpeningBalances);
    final BigDecimal periodChangePercent =
        JbhMoneyUtils.calculatePercentageChange(lastClosingBalances, firstOpeningBalances);

    final BalanceHistorySummaryResponse summary =
        new BalanceHistorySummaryResponse(
            totalBalanceSummary,
            periodChange,
            periodChangePercent,
            avgGrowthRateSummary,
            totalMovementsSummary);

    return new BalanceHistoryResponse(summary, balancesResponse);
  }

  @Override
  /** Orchestration: Coordinating calls to Application Services and Domain Services */
  public BalanceHistoryResponse findBalanceHistoryByUser(
      final UUID userId,
      final YearMonth startPeriod,
      final YearMonth inputEndPeriod,
      final YearMonth today)
      throws BusinessException {

    validatePeriodRange(startPeriod, inputEndPeriod);

    final List<ProductDTO> allProducts = productService.findActiveByUserId(userId);
    return findBalanceHistory(startPeriod, inputEndPeriod, today, allProducts);
  }

  private void validatePeriodRange(final YearMonth startPeriod, final YearMonth endPeriod)
      throws BusinessException {
    if (startPeriod == null || endPeriod == null) {
      throw new BusinessException(INVALID_RANGE_DATES_FOR_MONTHLY_BALANCES);
    }

    if (startPeriod.isAfter(endPeriod)) {
      throw new BusinessException(INVALID_RANGE_DATES_FOR_MONTHLY_BALANCES);
    }

    if (endPeriod.isAfter(YearMonth.now())) {
      throw new BusinessException(INVALID_RANGE_DATES_FOR_MONTHLY_BALANCES);
    }
  }
}

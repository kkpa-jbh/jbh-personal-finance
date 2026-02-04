package com.jbh.products.application.core.ports.input;

import static com.jbh.commons.util.JbhMoneyUtils.JBH_ZERO;
import static com.jbh.products.application.core.exceptions.BusinessApplicationExceptionType.INVALID_RANGE_DATES_FOR_MONTHLY_BALANCES;

import com.jbh.commons.exception.BusinessException;
import com.jbh.commons.util.JbhMoneyUtils;
import com.jbh.products.application.core.dto.MonthlyBalanceDTO;
import com.jbh.products.application.core.dto.ProductDTO;
import com.jbh.products.application.core.dto.balancehistory.BalanceHistoryResponseDTO;
import com.jbh.products.application.core.dto.balancehistory.BalanceHistorySummary;
import com.jbh.products.application.core.dto.balancehistory.MonthlyBalanceResponseDTO;
import com.jbh.products.application.core.services.account.ProductsService;
import com.jbh.products.application.core.services.monthlybalance.MonthlyBalanceService;
import com.jbh.products.application.core.usecases.FindMonthlyBalanceUseCase;
import com.jbh.products.domain.vo.PeriodRange;
import com.jbh.products.domain.vo.ProductId;
import com.jbh.products.domain.vo.ProductPK;
import java.math.BigDecimal;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

public class FindMonthlyBalanceInputPort implements FindMonthlyBalanceUseCase {

  private final MonthlyBalanceService monthlyBalanceService;
  private final ProductsService productService;

  public FindMonthlyBalanceInputPort(
      final MonthlyBalanceService monthlyBalanceService, final ProductsService productService) {
    this.monthlyBalanceService = monthlyBalanceService;
    this.productService = productService;
  }

  public List<MonthlyBalanceDTO> findByAccountAndPeriods(
      final ProductPK accountPK, final YearMonth startPeriod, final YearMonth endPeriod)
      throws BusinessException {

    validatePeriodRange(startPeriod, endPeriod);
    productService.findByUserAndProductId(accountPK.userId(), accountPK.accountId());

    return monthlyBalanceService.findByAccountAndPeriods(accountPK, startPeriod, endPeriod);
  }

  @Override
  public BalanceHistoryResponseDTO findHistoryByProduct(
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

  private BalanceHistoryResponseDTO findBalanceHistory(
      final YearMonth startPeriod,
      final YearMonth inputEndPeriod,
      final YearMonth today,
      final List<ProductDTO> allProducts) {
    if (allProducts.isEmpty()) {
      return BalanceHistoryResponseDTO.empty();
    }

    final Map<ProductId, ProductDTO> productsMap =
        allProducts.stream().collect(Collectors.toMap(ProductDTO::id, productDTO -> productDTO));
    final List<ProductId> productIds = allProducts.stream().map(ProductDTO::id).toList();

    final PeriodRange periodRange = PeriodRange.autoExclusive(startPeriod, inputEndPeriod, today);

    final List<MonthlyBalanceDTO> allMonthlyBalances =
        monthlyBalanceService.findByProductIdsAndPeriods(productIds, periodRange);

    if (allMonthlyBalances.isEmpty()) {
      return BalanceHistoryResponseDTO.empty();
    }
    // Logic by transforming the fetched data.

    BigDecimal totalBalanceSummary = JBH_ZERO;

    BigDecimal firstOpeningBalances = JBH_ZERO;
    BigDecimal lastClosingBalances = JBH_ZERO;
    BigDecimal weightedGrowthSum = JBH_ZERO;
    BigDecimal totalWeightForAvg = JBH_ZERO;
    int totalMovementsSummary = 0;

    final List<MonthlyBalanceResponseDTO> balancesResponse = new ArrayList<>();

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
          MonthlyBalanceResponseDTO.fromDTO(
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

    final BalanceHistorySummary summary =
        new BalanceHistorySummary(
            totalBalanceSummary,
            periodChange,
            periodChangePercent,
            avgGrowthRateSummary,
            totalMovementsSummary);

    return new BalanceHistoryResponseDTO(summary, balancesResponse);
  }

  @Override
  /** Orchestration: Coordinating calls to Application Services and Domain Services */
  public BalanceHistoryResponseDTO findBalanceHistoryByUser(
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

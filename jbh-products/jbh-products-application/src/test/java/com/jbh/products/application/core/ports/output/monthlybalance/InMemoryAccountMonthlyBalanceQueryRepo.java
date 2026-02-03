package com.jbh.products.application.core.ports.output.monthlybalance;

import com.jbh.products.application.core.comparator.AccountMonthlyBalanceComparators;
import com.jbh.products.application.core.dto.MonthlyBalanceDTO;
import com.jbh.products.application.core.mappers.MonthlyBalanceMapper;
import com.jbh.products.domain.vo.ProductId;
import com.jbh.products.domain.vo.ProductPK;
import java.math.BigDecimal;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class InMemoryAccountMonthlyBalanceQueryRepo implements AccountMonthlyBalanceQueryRepo {

  private final Logger log = LoggerFactory.getLogger(InMemoryAccountMonthlyBalanceQueryRepo.class);

  private final Map<String, MonthlyBalanceDTO> storage = new HashMap<>();

  public void saveAll(final List<MonthlyBalanceDTO> balances) {
    balances.forEach(this::save);
  }

  public void save(final MonthlyBalanceDTO balance) {
    final String key = generateKey(balance.accountId(), balance.year(), balance.month());
    log.warn("Saving balance " + balance);
    storage.put(key, balance);
  }

  private String generateKey(final ProductId accountId, final Integer year, final Integer month) {
    return accountId.value() + "_" + year + "_" + month;
  }

  public void clearStorage() {
    storage.clear();
  }

  public int size() {
    return storage.size();
  }

  public List<MonthlyBalanceDTO> findAll() {
    return new ArrayList<>(storage.values());
  }

  @Override
  public List<MonthlyBalanceDTO> findByAccountAndPeriods(
      final ProductPK accountPK, final YearMonth startPeriod, final YearMonth endPeriod) {
    return storage.values().stream()
        .filter(balance -> balance.accountId().equals(accountPK.accountId()))
        .filter(
            balance ->
                balance.period().isAfter(startPeriod) && balance.period().isBefore(endPeriod))
        .sorted((b1, b2) -> b1.period().compareTo(b2.period()))
        .toList();
  }

  private List<MonthlyBalanceDTO> findByAccountId(final ProductId accountId) {
    return storage.values().stream()
        .filter(balance -> balance.accountId().equals(accountId))
        .sorted((b1, b2) -> b1.period().compareTo(b2.period()))
        .toList();
  }

  @Override
  public Optional<MonthlyBalanceDTO> findByAccountIdYearAndMonth(
      final ProductId accountId, final Integer balanceYear, final Integer balanceMonth) {

    final String key = generateKey(accountId, balanceYear, balanceMonth);
    final Optional<MonthlyBalanceDTO> result = Optional.ofNullable(storage.get(key));
    log.info("Find By Period {} result: {}", YearMonth.of(balanceYear, balanceMonth), result);
    return result;
  }

  @Override
  public Optional<MonthlyBalanceDTO> findByAccountIdAndPeriod(
      final ProductId accountId, final YearMonth period) {
    return findByAccountIdYearAndMonth(accountId, period.getYear(), period.getMonthValue());
  }

  @Override
  public List<MonthlyBalanceDTO> findNextBalancesFromPeriodInclusive(
      final ProductId accountId, final YearMonth currentPeriod) {

    return storage.values().stream()
        .filter(balance -> balance.accountId().equals(accountId))
        .filter(balance -> !balance.period().isBefore(currentPeriod))
        .sorted((b1, b2) -> b1.period().compareTo(b2.period()))
        .toList();
  }

  @Override
  public List<MonthlyBalanceDTO> findAllByAccountIdUntilNow(final ProductId accountId) {
    return findByAccountId(accountId).stream()
        .filter(balance -> balance.period().isBefore(YearMonth.now().plusMonths(1)))
        .sorted((b1, b2) -> b1.period().compareTo(b2.period()))
        .toList();
  }

  @Override
  public Optional<MonthlyBalanceDTO> findLastOfficialReport(final ProductId accountId) {
    return findByAccountId(accountId).stream()
        .filter(MonthlyBalanceDTO::officialMonthlyReport)
        .map(MonthlyBalanceMapper::toDomain)
        .toList()
        .stream()
        .min(AccountMonthlyBalanceComparators.BY_PERIOD_DESC)
        .map(MonthlyBalanceMapper::toDTO);
  }

  @Override
  public BigDecimal sumNetProfitOfficialReported(final ProductId accountId) {

    return findByAccountId(accountId).stream()
        .filter(MonthlyBalanceDTO::officialMonthlyReport)
        .map(MonthlyBalanceDTO::monthlyNetProfit)
        .filter(Objects::nonNull)
        .reduce(BigDecimal.ZERO, BigDecimal::add);
  }

  @Override
  public List<MonthlyBalanceDTO> findByProductIdsAndPeriods(
      final List<ProductId> productIds, final YearMonth startPeriod, final YearMonth endPeriod) {

    if (productIds == null || productIds.isEmpty()) {
      return new ArrayList<>();
    }

    final Map<ProductId, List<MonthlyBalanceDTO>> result = new HashMap<>();

    final List<MonthlyBalanceDTO> allBalances = new ArrayList<>();
    for (final ProductId productId : productIds) {
      final List<MonthlyBalanceDTO> balances =
          storage.values().stream()
              .filter(balance -> balance.accountId().equals(productId))
              .filter(
                  balance ->
                      !balance.period().isBefore(startPeriod)
                          && !balance.period().isAfter(endPeriod))
              .sorted((b1, b2) -> b1.period().compareTo(b2.period()))
              .toList();

      if (!balances.isEmpty()) {
        result.put(productId, balances);
      }
      allBalances.addAll(balances);
    }

    return allBalances;
  }
}

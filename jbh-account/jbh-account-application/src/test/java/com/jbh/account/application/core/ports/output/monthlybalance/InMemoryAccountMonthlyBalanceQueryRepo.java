package com.jbh.account.application.core.ports.output.monthlybalance;

import com.jbh.account.application.core.comparator.AccountMonthlyBalanceComparators;
import com.jbh.account.application.core.dto.AccountMonthlyBalanceDTO;
import com.jbh.account.application.core.mappers.MonthlyBalanceMapper;
import com.jbh.account.domain.vo.AccountId;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class InMemoryAccountMonthlyBalanceQueryRepo implements AccountMonthlyBalanceQueryRepo {

  private final Map<String, AccountMonthlyBalanceDTO> storage = new HashMap<>();

  public void saveAll(final List<AccountMonthlyBalanceDTO> balances) {
    balances.forEach(this::save);
  }

  public void save(final AccountMonthlyBalanceDTO balance) {
    final String key = generateKey(balance.accountId(), balance.year(), balance.month());
    storage.put(key, balance);
  }

  private String generateKey(final AccountId accountId, final Integer year, final Integer month) {
    return accountId.value() + "_" + year + "_" + month;
  }

  public void clearStorage() {
    storage.clear();
  }

  public int size() {
    return storage.size();
  }

  public List<AccountMonthlyBalanceDTO> findAll() {
    return new ArrayList<>(storage.values());
  }

  public List<AccountMonthlyBalanceDTO> findByAccountId(final AccountId accountId) {
    return storage.values().stream()
        .filter(balance -> balance.accountId().equals(accountId))
        .sorted((b1, b2) -> b1.period().compareTo(b2.period()))
        .toList();
  }

  @Override
  public Optional<AccountMonthlyBalanceDTO> findByAccountIdYearAndMonth(
      final AccountId accountId, final Integer balanceYear, final Integer balanceMonth) {

    final String key = generateKey(accountId, balanceYear, balanceMonth);
    return Optional.ofNullable(storage.get(key));
  }

  @Override
  public Optional<AccountMonthlyBalanceDTO> findByAccountIdAndPeriod(
      final AccountId accountId, final YearMonth period) {
    return findByAccountIdYearAndMonth(accountId, period.getYear(), period.getMonthValue());
  }

  @Override
  public List<AccountMonthlyBalanceDTO> findNextBalancesFromPeriodInclusive(
      final AccountId accountId, final YearMonth currentPeriod) {

    return storage.values().stream()
        .filter(balance -> balance.accountId().equals(accountId))
        .filter(balance -> !balance.period().isBefore(currentPeriod))
        .sorted((b1, b2) -> b1.period().compareTo(b2.period()))
        .toList();
  }

  @Override
  public Optional<AccountMonthlyBalanceDTO> findLastOfficialReport(final AccountId accountId) {
    return findByAccountId(accountId).stream().map(MonthlyBalanceMapper::toDomain).toList().stream()
        .min(AccountMonthlyBalanceComparators.BY_PERIOD_DESC)
        .map(MonthlyBalanceMapper::toDTO);
  }
}

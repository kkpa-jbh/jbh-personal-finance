package com.jbh.account.application.accounts.ports.output.monthlybalance.inmemory;

import com.jbh.account.application.accounts.ports.output.monthlybalance.AccountMonthlyBalanceQueryRepo;
import com.jbh.account.domain.vo.AccountId;
import com.jbh.account.domain.vo.AccountMonthlyBalanceDTO;
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
    final String key = generateKey(balance.getAccountId(), balance.getYear(), balance.getMonth());
    storage.put(key, balance);
  }  @Override
  public Optional<AccountMonthlyBalanceDTO> findByAccountIdYearAndMonth(
      final AccountId accountId, final Integer balanceYear, final Integer balanceMonth) {

    final String key = generateKey(accountId, balanceYear, balanceMonth);
    return Optional.ofNullable(storage.get(key));
  }

  private String generateKey(final AccountId accountId, final Integer year, final Integer month) {
    return accountId.value() + "_" + year + "_" + month;
  }

  public void clearStorage() {
    storage.clear();
  }  @Override
  public Optional<AccountMonthlyBalanceDTO> findByAccountIdAndPeriod(
      final AccountId accountId, final YearMonth period) {
    return findByAccountIdYearAndMonth(accountId, period.getYear(), period.getMonthValue());
  }

  public int size() {
    return storage.size();
  }

  public List<AccountMonthlyBalanceDTO> findAll() {
    return new ArrayList<>(storage.values());
  }  @Override
  public List<AccountMonthlyBalanceDTO> findNextBalancesFromPeriodInclusive(
      final AccountId accountId, final YearMonth currentPeriod) {

    return storage.values().stream()
        .filter(balance -> balance.getAccountId().equals(accountId))
        .filter(balance -> !balance.getPeriod().isBefore(currentPeriod))
        .sorted((b1, b2) -> b1.getPeriod().compareTo(b2.getPeriod()))
        .toList();
  }

  public List<AccountMonthlyBalanceDTO> findByAccountId(final AccountId accountId) {
    return storage.values().stream()
        .filter(balance -> balance.getAccountId().equals(accountId))
        .sorted((b1, b2) -> b1.getPeriod().compareTo(b2.getPeriod()))
        .toList();
  }






}

package com.jbh.account.application.core.ports.output.monthlybalance;

import com.jbh.account.application.core.dto.AccountMonthlyBalanceDTO;
import java.util.List;

public class InMemoryAccountMonthlyBalanceWriterRepository
    implements AccountMonthlyBalanceWriterRepository {

  private final InMemoryAccountMonthlyBalanceQueryRepo queryRepo;

  public InMemoryAccountMonthlyBalanceWriterRepository(
      final InMemoryAccountMonthlyBalanceQueryRepo queryRepo) {
    this.queryRepo = queryRepo;
  }

  @Override
  public void saveBalance(final AccountMonthlyBalanceDTO accountMonthlyBalance) {
    queryRepo.save(accountMonthlyBalance);
  }

  @Override
  public List<AccountMonthlyBalanceDTO> saveMultiBalances(
      final List<AccountMonthlyBalanceDTO> accountMonthlyBalance) {
    queryRepo.saveAll(accountMonthlyBalance);
    return accountMonthlyBalance;
  }
}

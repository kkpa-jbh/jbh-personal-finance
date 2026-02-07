package com.jbh.products.application.core.ports.output.monthlybalance;

import com.jbh.products.application.feature.monthlybalance.dto.MonthlyBalanceDTO;
import com.jbh.products.application.feature.monthlybalance.ports.output.AccountMonthlyBalanceWriterRepository;
import java.util.List;

public class InMemoryAccountMonthlyBalanceWriterRepository
    implements AccountMonthlyBalanceWriterRepository {

  private final InMemoryAccountMonthlyBalanceQueryRepo queryRepo;

  public InMemoryAccountMonthlyBalanceWriterRepository(
      final InMemoryAccountMonthlyBalanceQueryRepo queryRepo) {
    this.queryRepo = queryRepo;
  }

  @Override
  public void saveBalance(final MonthlyBalanceDTO accountMonthlyBalance) {
    queryRepo.save(accountMonthlyBalance);
  }

  @Override
  public List<MonthlyBalanceDTO> saveMultiBalances(
      final List<MonthlyBalanceDTO> accountMonthlyBalance) {
    queryRepo.saveAll(accountMonthlyBalance);
    return accountMonthlyBalance;
  }
}

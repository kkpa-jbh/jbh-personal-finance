package com.jbh.finance.test.testfixtures.fakes.monthlybalance;

import com.jbh.finance.application.feature.monthlybalance.dto.MonthlyBalanceDTO;
import com.jbh.finance.application.feature.monthlybalance.ports.output.MonthlyBalanceWriterRepo;
import java.util.List;

public class InMemoryProductMonthlyBalanceWriterRepository implements MonthlyBalanceWriterRepo {

  private final InMemoryMonthlyBalanceQueryRepo queryRepo;

  public InMemoryProductMonthlyBalanceWriterRepository(
      final InMemoryMonthlyBalanceQueryRepo queryRepo) {
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

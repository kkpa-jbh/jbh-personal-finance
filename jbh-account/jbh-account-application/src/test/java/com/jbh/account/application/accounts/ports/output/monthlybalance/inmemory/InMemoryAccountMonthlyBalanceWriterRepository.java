package com.jbh.account.application.accounts.ports.output.monthlybalance.inmemory;

import com.jbh.account.application.accounts.ports.output.monthlybalance.AccountMonthlyBalanceWriterRepository;
import com.jbh.account.domain.vo.AccountMonthlyBalanceDTO;
import java.util.List;

public class InMemoryAccountMonthlyBalanceWriterRepository implements AccountMonthlyBalanceWriterRepository {

  private final InMemoryAccountMonthlyBalanceQueryRepo queryRepo;

  public InMemoryAccountMonthlyBalanceWriterRepository(InMemoryAccountMonthlyBalanceQueryRepo queryRepo) {
    this.queryRepo = queryRepo;
  }

  @Override
  public void saveBalance(AccountMonthlyBalanceDTO accountMonthlyBalance) {
    queryRepo.save(accountMonthlyBalance);
  }

  @Override
  public List<AccountMonthlyBalanceDTO> saveMultiBalances(List<AccountMonthlyBalanceDTO> accountMonthlyBalance) {
    queryRepo.saveAll(accountMonthlyBalance);
    return accountMonthlyBalance;
  }
}
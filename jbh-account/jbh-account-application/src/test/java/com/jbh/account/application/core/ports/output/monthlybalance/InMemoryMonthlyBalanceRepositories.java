package com.jbh.account.application.core.ports.output.monthlybalance;

public class InMemoryMonthlyBalanceRepositories {

  private final InMemoryAccountMonthlyBalanceQueryRepo queryRepo;
  private final InMemoryAccountMonthlyBalanceWriterRepository writerRepo;

  public InMemoryMonthlyBalanceRepositories() {
    this.queryRepo = new InMemoryAccountMonthlyBalanceQueryRepo();
    this.writerRepo = new InMemoryAccountMonthlyBalanceWriterRepository(queryRepo);
  }

  public InMemoryAccountMonthlyBalanceQueryRepo getQueryRepo() {
    return queryRepo;
  }

  public InMemoryAccountMonthlyBalanceWriterRepository getWriterRepo() {
    return writerRepo;
  }

  public void clearStorage() {
    queryRepo.clearStorage();
  }
}

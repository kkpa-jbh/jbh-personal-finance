package com.jbh.finance.test.testfixtures.fakes.monthlybalance;

public class InMemoryMonthlyBalanceRepositories {

  private final InMemoryMonthlyBalanceQueryRepo queryRepo;
  private final InMemoryProductMonthlyBalanceWriterRepository writerRepo;

  public InMemoryMonthlyBalanceRepositories() {
    this.queryRepo = new InMemoryMonthlyBalanceQueryRepo();
    this.writerRepo = new InMemoryProductMonthlyBalanceWriterRepository(queryRepo);
  }

  public InMemoryMonthlyBalanceQueryRepo getQueryRepo() {
    return queryRepo;
  }

  public InMemoryProductMonthlyBalanceWriterRepository getWriterRepo() {
    return writerRepo;
  }

  public void clearStorage() {
    queryRepo.clearStorage();
  }
}

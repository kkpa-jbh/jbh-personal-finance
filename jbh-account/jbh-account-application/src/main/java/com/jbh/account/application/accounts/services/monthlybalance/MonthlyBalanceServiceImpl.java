package com.jbh.account.application.accounts.services.monthlybalance;

import com.jbh.account.application.accounts.ports.output.monthlybalance.AccountMonthlyBalanceQueryRepo;
import com.jbh.account.application.accounts.ports.output.monthlybalance.AccountMonthlyBalanceWriterRepository;
import com.jbh.account.domain.entity.AccountMonthlyBalanceDomain;
import com.jbh.account.domain.vo.AccountId;
import com.jbh.account.domain.vo.AccountMonthlyBalanceDTO;
import java.time.YearMonth;
import java.util.List;
import java.util.Optional;

public class MonthlyBalanceServiceImpl implements MonthlyBalanceService {
  private final AccountMonthlyBalanceQueryRepo queryRepo;
  private final AccountMonthlyBalanceWriterRepository writerRepo;

  public MonthlyBalanceServiceImpl(
      final AccountMonthlyBalanceQueryRepo monthlyBalanceRepo,
      final AccountMonthlyBalanceWriterRepository monthlyBalanceWriterRepo) {
    this.writerRepo = monthlyBalanceWriterRepo;
    this.queryRepo = monthlyBalanceRepo;
  }

  @Override
  public Optional<AccountMonthlyBalanceDTO> findByAccountIdYearAndMonth(
      final AccountId accountId, final Integer balanceYear, final Integer balanceMonth) {
    return queryRepo.findByAccountIdYearAndMonth(accountId, balanceYear, balanceMonth);
  }

  @Override
  public Optional<AccountMonthlyBalanceDTO> findByAccountIdAndPeriod(
      final AccountId accountId, final YearMonth period) {
    return queryRepo.findByAccountIdAndPeriod(accountId, period);
  }

  @Override
  public List<AccountMonthlyBalanceDTO> findNextBalancesFromPeriodInclusive(
      final AccountId accountId, final YearMonth currentPeriod) {
    return queryRepo.findNextBalancesFromPeriodInclusive(accountId, currentPeriod);
  }

  @Override
  public AccountMonthlyBalanceDTO updateOpeningBalanceNextMonth(
      final AccountMonthlyBalanceDTO currentMonthlyBalance) {
    final YearMonth nextPeriod = currentMonthlyBalance.getPeriod().plusMonths(1);

    final AccountMonthlyBalanceDTO nextMonthlyBalance =
        queryRepo
            .findByAccountIdYearAndMonth(
                currentMonthlyBalance.getAccountId(),
                nextPeriod.getYear(),
                nextPeriod.getMonthValue())
            .orElseGet(
                () ->
                    AccountMonthlyBalanceDomain.withPeriod(
                            currentMonthlyBalance.getAccountId(),
                            nextPeriod.getYear(),
                            nextPeriod.getMonthValue())
                        .toDTO());

    nextMonthlyBalance.adjustOpeningBalance(currentMonthlyBalance.getClosingBalance());
    saveBalance(nextMonthlyBalance);

    return nextMonthlyBalance;
  }

  @Override
  public void saveBalance(final AccountMonthlyBalanceDTO accountMonthlyBalance) {
    writerRepo.saveBalance(accountMonthlyBalance);
  }

  @Override
  public List<AccountMonthlyBalanceDTO> saveMultiBalances(
      final List<AccountMonthlyBalanceDTO> accountMonthlyBalance) {
    return writerRepo.saveMultiBalances(accountMonthlyBalance);
  }
}

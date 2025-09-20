package com.jbh.account.application.accounts.services.monthlybalance;

import static com.jbh.account.application.accounts.mappers.MonthlyBalanceMapper.toDomain;

import com.jbh.account.application.accounts.dto.AccountMonthlyBalanceDTO;
import com.jbh.account.application.accounts.mappers.MonthlyBalanceMapper;
import com.jbh.account.application.accounts.ports.output.monthlybalance.AccountMonthlyBalanceQueryRepo;
import com.jbh.account.application.accounts.ports.output.monthlybalance.AccountMonthlyBalanceWriterRepository;
import com.jbh.account.domain.entity.AccountMonthlyBalanceDomain;
import com.jbh.account.domain.vo.AccountId;
import java.time.YearMonth;
import java.util.List;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MonthlyBalanceServiceImpl implements MonthlyBalanceService {
  private static final Logger LOG = LoggerFactory.getLogger(MonthlyBalanceServiceImpl.class);
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
    final YearMonth nextPeriod = currentMonthlyBalance.period().plusMonths(1);

    final AccountMonthlyBalanceDomain nextMonthlyBalance =
        queryRepo
            .findByAccountIdYearAndMonth(
                currentMonthlyBalance.accountId(), nextPeriod.getYear(), nextPeriod.getMonthValue())
            .map(MonthlyBalanceMapper::toDomain)
            .orElseGet(
                () ->
                    AccountMonthlyBalanceDomain.withPeriod(
                        currentMonthlyBalance.accountId(),
                        nextPeriod.getYear(),
                        nextPeriod.getMonthValue()));

    nextMonthlyBalance.adjustOpeningBalance(toDomain(currentMonthlyBalance));

    final AccountMonthlyBalanceDTO nextMonthlyBalanceDTO =
        MonthlyBalanceMapper.toDTO(nextMonthlyBalance);
    saveBalance(nextMonthlyBalanceDTO);

    return nextMonthlyBalanceDTO;
  }

  @Override
  public void saveBalance(final AccountMonthlyBalanceDTO accountMonthlyBalance) {
    writerRepo.saveBalance(accountMonthlyBalance);
  }

  @Override
  public List<AccountMonthlyBalanceDTO> saveMultiBalances(
      final List<AccountMonthlyBalanceDTO> accountMonthlyBalance) {
    LOG.info(
        "Persisting in database Monthly Balances {}",
        accountMonthlyBalance.stream().map(AccountMonthlyBalanceDTO::period).toList());
    final List<AccountMonthlyBalanceDTO> savedBalances =
        writerRepo.saveMultiBalances(accountMonthlyBalance);
    LOG.info("Monthly Balances persisted successfully");
    return savedBalances;
  }
}

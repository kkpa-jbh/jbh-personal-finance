package com.jbh.account.application.core.services.monthlybalance;

import static com.jbh.account.application.core.mappers.MonthlyBalanceMapper.toDomain;

import com.jbh.account.application.core.dto.MonthlyBalanceDTO;
import com.jbh.account.application.core.mappers.MonthlyBalanceMapper;
import com.jbh.account.application.core.ports.output.monthlybalance.AccountMonthlyBalanceQueryRepo;
import com.jbh.account.application.core.ports.output.monthlybalance.AccountMonthlyBalanceWriterRepository;
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
  public Optional<MonthlyBalanceDTO> findByAccountIdYearAndMonth(
      final AccountId accountId, final Integer balanceYear, final Integer balanceMonth) {
    return queryRepo.findByAccountIdYearAndMonth(accountId, balanceYear, balanceMonth);
  }

  @Override
  public Optional<MonthlyBalanceDTO> findByAccountIdAndPeriod(
      final AccountId accountId, final YearMonth period) {
    return queryRepo.findByAccountIdAndPeriod(accountId, period);
  }

  @Override
  public List<MonthlyBalanceDTO> findNextBalancesFromPeriodInclusive(
      final AccountId accountId, final YearMonth currentPeriod) {
    return queryRepo.findNextBalancesFromPeriodInclusive(accountId, currentPeriod);
  }

  @Override
  public Optional<MonthlyBalanceDTO> findLastOfficialReport(final AccountId accountId) {
    return queryRepo.findLastOfficialReport(accountId);
  }

  @Override
  public MonthlyBalanceDTO updateOpeningBalanceNextMonth(
      final MonthlyBalanceDTO currentMonthlyBalance) {
    final YearMonth nextPeriod = currentMonthlyBalance.period().plusMonths(1);

    final AccountMonthlyBalanceDomain nextMonthlyBalance =
        queryRepo
            .findByAccountIdYearAndMonth(
                currentMonthlyBalance.accountId(), nextPeriod.getYear(), nextPeriod.getMonthValue())
            .map(MonthlyBalanceMapper::toDomain)
            .orElseGet(
                () ->
                    AccountMonthlyBalanceDomain.withPeriod(
                        currentMonthlyBalance.accountId(), nextPeriod));

    nextMonthlyBalance.assignOpeningBalance(toDomain(currentMonthlyBalance));

    final MonthlyBalanceDTO nextMonthlyBalanceDTO = MonthlyBalanceMapper.toDTO(nextMonthlyBalance);
    saveBalance(nextMonthlyBalanceDTO);

    return nextMonthlyBalanceDTO;
  }

  @Override
  public boolean isLastOfficialReport(final MonthlyBalanceDTO monthlyBalanceDTO) {
    final Optional<MonthlyBalanceDTO> latestOfficialMonthlyReport =
        findLastOfficialReport(monthlyBalanceDTO.accountId());
    final YearMonth currentPeriod = monthlyBalanceDTO.period();

    final boolean isLastOfficialReport =
        latestOfficialMonthlyReport
            .map(
                accountMonthlyBalanceDTO -> accountMonthlyBalanceDTO.period().equals(currentPeriod))
            .orElse(false);

    LOG.info("Its period {} the last official report: {}", currentPeriod, isLastOfficialReport);
    return isLastOfficialReport;
  }

  @Override
  public void saveBalance(final MonthlyBalanceDTO accountMonthlyBalance) {
    writerRepo.saveBalance(accountMonthlyBalance);
  }

  @Override
  public List<MonthlyBalanceDTO> saveMultiBalances(
      final List<MonthlyBalanceDTO> accountMonthlyBalance) {
    LOG.info(
        "Persisting in database Monthly Balances {}",
        accountMonthlyBalance.stream().map(MonthlyBalanceDTO::period).toList());
    final List<MonthlyBalanceDTO> savedBalances =
        writerRepo.saveMultiBalances(accountMonthlyBalance);
    LOG.info("Monthly Balances persisted successfully");
    return savedBalances;
  }
}

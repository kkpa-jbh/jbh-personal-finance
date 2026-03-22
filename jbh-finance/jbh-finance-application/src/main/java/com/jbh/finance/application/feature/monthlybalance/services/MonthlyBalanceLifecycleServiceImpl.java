package com.jbh.finance.application.feature.monthlybalance.services;

import static com.jbh.finance.application.feature.monthlybalance.mappers.MonthlyBalanceMapper.toDTO;
import static com.jbh.finance.application.feature.monthlybalance.mappers.MonthlyBalanceMapper.toDomain;

import com.jbh.commons.exception.BusinessException;
import com.jbh.finance.application.feature.monthlybalance.commands.AddMonthlyBalanceCommand;
import com.jbh.finance.application.feature.monthlybalance.dto.MonthlyBalanceDTO;
import com.jbh.finance.application.feature.monthlybalance.mappers.MonthlyBalanceMapper;
import com.jbh.finance.application.feature.monthlybalance.ports.output.MonthlyBalanceQueryRepo;
import com.jbh.finance.application.feature.monthlybalance.ports.output.MonthlyBalanceWriterRepo;
import com.jbh.finance.domain.monthlybalance.MonthlyBalanceDomain;
import com.jbh.finance.domain.product.vo.ProductId;
import com.jbh.finance.domain.product.vo.ProductPK;
import com.jbh.finance.domain.shared.vo.PeriodRange;
import java.math.BigDecimal;
import java.time.YearMonth;
import java.util.List;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MonthlyBalanceLifecycleServiceImpl implements MonthlyBalanceLifecycleService {

  private final static Logger LOG = LoggerFactory.getLogger(MonthlyBalanceLifecycleServiceImpl.class);
  private final MonthlyBalanceQueryRepo queryRepo;
  private final MonthlyBalanceWriterRepo writerRepo;

  public MonthlyBalanceLifecycleServiceImpl(
      final MonthlyBalanceQueryRepo monthlyBalanceRepo,
      final MonthlyBalanceWriterRepo monthlyBalanceWriterRepo) {
    this.writerRepo = monthlyBalanceWriterRepo;
    this.queryRepo = monthlyBalanceRepo;
  }

  @Override
  public List<MonthlyBalanceDTO> findByAccountAndPeriods(
      final ProductPK accountPK, final YearMonth startPeriod, final YearMonth endPeriod) {
    return queryRepo.findByAccountAndPeriods(accountPK, startPeriod, endPeriod);
  }

  @Override
  public Optional<MonthlyBalanceDTO> findByAccountIdYearAndMonth(
      final ProductId accountId, final Integer balanceYear, final Integer balanceMonth) {
    return queryRepo.findByAccountIdYearAndMonth(accountId, balanceYear, balanceMonth);
  }

  @Override
  public Optional<MonthlyBalanceDTO> findByAccountIdAndPeriod(
      final ProductId accountId, final YearMonth period) {
    return queryRepo.findByAccountIdAndPeriod(accountId, period);
  }

  @Override
  public Optional<MonthlyBalanceDTO> findLastOfficialReport(final ProductId accountId) {
    return queryRepo.findLastOfficialReport(accountId);
  }

  @Override
  public BigDecimal sumNetProfitOfficialReported(final ProductId accountId) {
    return queryRepo.sumNetProfitOfficialReported(accountId);
  }

  @Override
  public List<MonthlyBalanceDTO> findNextBalancesFromPeriodInclusive(
      final ProductId accountId, final YearMonth currentPeriod) {
    return queryRepo.findNextBalancesFromPeriodInclusive(accountId, currentPeriod);
  }

  @Override
  public List<MonthlyBalanceDTO> findAllByAccountIdUntilNow(final ProductId accountId) {
    // TODO: Check if it's necessary to return until the current period
    return queryRepo.findAllByAccountIdUntilNow(accountId);
  }

  @Override
  public List<MonthlyBalanceDTO> findByProductIdsAndPeriods(
      final List<ProductId> productIds, final PeriodRange periodRange) {
    return queryRepo.findByProductIdsAndPeriods(productIds, periodRange);
  }

  @Override
  public MonthlyBalanceDTO updateOfficialReportedBalance(
      final MonthlyBalanceDTO reportedMonthlyBalance, final AddMonthlyBalanceCommand command)
      throws BusinessException {

    final MonthlyBalanceDomain monthlyBalanceDomain = toDomain(reportedMonthlyBalance);
    final var updatedMonthlyBalance = assignOfficialReport(monthlyBalanceDomain, command);

    saveBalance(updatedMonthlyBalance);
    updateOpeningBalanceNextMonth(updatedMonthlyBalance);

    return updatedMonthlyBalance;
  }

  private MonthlyBalanceDTO assignOfficialReport(
      final MonthlyBalanceDomain monthlyBalanceDomain, final AddMonthlyBalanceCommand command)
      throws BusinessException {
    monthlyBalanceDomain.assignOfficialMonthlyReport(
        command.closingBalance(),
        command.monthlyProfitReported(),
        command.incomeWithholdingTaxAmount());
    return toDTO(monthlyBalanceDomain);
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

  @Override
  public void updateOpeningBalanceNextMonth(final MonthlyBalanceDTO currentMonthlyBalance) {
    final YearMonth nextPeriod = currentMonthlyBalance.period().plusMonths(1);

    final MonthlyBalanceDomain nextMonthlyBalance =
        this.findByAccountIdYearAndMonth(
                currentMonthlyBalance.productId(), nextPeriod.getYear(), nextPeriod.getMonthValue())
            .map(MonthlyBalanceMapper::toDomain)
            .orElseGet(
                () ->
                    MonthlyBalanceDomain.withPeriod(currentMonthlyBalance.productId(), nextPeriod));

    nextMonthlyBalance.assignOpeningBalance(toDomain(currentMonthlyBalance));

    final MonthlyBalanceDTO nextMonthlyBalanceDTO = toDTO(nextMonthlyBalance);
    saveBalance(nextMonthlyBalanceDTO);
  }

  @Override
  public boolean isLastOfficialReport(final MonthlyBalanceDTO monthlyBalanceDTO) {
    final Optional<MonthlyBalanceDTO> latestOfficialMonthlyReport =
        this.findLastOfficialReport(monthlyBalanceDTO.productId());
    final YearMonth currentPeriod = monthlyBalanceDTO.period();

    final boolean isLastOfficialReport =
        latestOfficialMonthlyReport
            .map(
                accountMonthlyBalanceDTO -> accountMonthlyBalanceDTO.period().equals(currentPeriod))
            .orElse(false);

    LOG.info("Its period {} the last official report: {}", currentPeriod, isLastOfficialReport);
    return isLastOfficialReport;
  }
}

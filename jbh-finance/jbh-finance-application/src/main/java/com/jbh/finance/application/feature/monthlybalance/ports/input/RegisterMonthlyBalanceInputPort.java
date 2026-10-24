package com.jbh.finance.application.feature.monthlybalance.ports.input;

import com.jbh.commons.exception.BusinessException;
import com.jbh.finance.application.acid.UnitOfWork;
import com.jbh.finance.application.feature.category.dto.CategoryDTO;
import com.jbh.finance.application.feature.category.services.CategoryService;
import com.jbh.finance.application.feature.monthlybalance.commands.AddMonthlyBalanceCommand;
import com.jbh.finance.application.feature.monthlybalance.dto.MonthlyBalanceDTO;
import com.jbh.finance.application.feature.monthlybalance.mappers.MonthlyBalanceMapper;
import com.jbh.finance.application.feature.monthlybalance.services.MonthlyBalanceLifecycleService;
import com.jbh.finance.application.feature.monthlybalance.services.MonthlyBalanceProfitStrategy;
import com.jbh.finance.application.feature.monthlybalance.services.ProcessMonthlyBalanceService;
import com.jbh.finance.application.feature.monthlybalance.services.ReportedProfitStrategy;
import com.jbh.finance.application.feature.monthlybalance.services.UnreportedProfitStrategy;
import com.jbh.finance.application.feature.monthlybalance.usecases.RegisterMonthlyBalanceUseCase;
import com.jbh.finance.application.feature.movement.commands.AddMovementCommand;
import com.jbh.finance.application.feature.movement.services.ProcessMovementService;
import com.jbh.finance.application.feature.product.services.ProductLifecycleService;
import com.jbh.finance.application.shared.exceptions.BusinessApplicationExceptionType;
import com.jbh.finance.domain.monthlybalance.MonthlyBalanceDomain;
import com.jbh.finance.domain.movement.vo.MovementType;
import com.jbh.finance.domain.product.vo.ProductId;
import com.jbh.finance.domain.product.vo.ProductPK;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.Optional;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RegisterMonthlyBalanceInputPort implements RegisterMonthlyBalanceUseCase {

  private static final Logger log = LoggerFactory.getLogger(RegisterMonthlyBalanceInputPort.class);

  private final MonthlyBalanceLifecycleService monthlyBalanceService;
  private final ProductLifecycleService accountService;
  private final ProcessMovementService accountMovementService;
  private final CategoryService categoryService;
  private final UnitOfWork unitOfWork;
  private final ProcessMonthlyBalanceService processMonthlyBalanceService;

  private final CategoryDTO incomeInitialBalanceCat;

  public RegisterMonthlyBalanceInputPort(
      final MonthlyBalanceLifecycleService monthlyBalanceService,
      final ProductLifecycleService accountService,
      final ProcessMovementService accountMovementService,
      final CategoryService categoryService,
      final UnitOfWork unitOfWork,
      final ProcessMonthlyBalanceService processMonthlyBalanceService) {
    this.unitOfWork = unitOfWork;
    this.processMonthlyBalanceService = processMonthlyBalanceService;
    this.accountMovementService = accountMovementService;
    this.accountService = accountService;
    this.monthlyBalanceService = monthlyBalanceService;
    this.categoryService = categoryService;

    incomeInitialBalanceCat = this.categoryService.findIncomeInitialBalance();
  }

  /**
   * It worth suggest calling this use case once the next month has started with the profit
   * reported. The user registers a monthly balance once the month has ended. \n It isn't associated
   * with any category. \n The monthly balance can already exist with some movements associated. \n
   * The monthly profit is provided by the institution productDTO. \n Based on the monthly balances,
   * this method will calculate the netGrowthRate for the month. This method will also calculate the
   * monthlyExpenses for the month. After syncing the monthly balance, it should update the opening
   * balance of the next month.
   *
   * @param runningDate
   * @param userId
   * @param accountId
   * @param addMonthlyBalanceCommand
   * @return
   * @throws BusinessException
   */
  @Override
  public MonthlyBalanceDTO registerOfficialMonthlyBalance(
      final LocalDate runningDate,
      final UUID userId,
      final ProductId accountId,
      final AddMonthlyBalanceCommand addMonthlyBalanceCommand)
      throws BusinessException {

    // Command validation
    addMonthlyBalanceCommand.validate();

    final YearMonth periodToRegister = addMonthlyBalanceCommand.monthlyPeriod();
    log.info(
        "Registering Official Monthly Balance for productDTO {} on {}",
        accountId,
        periodToRegister);

    validatePeriod(runningDate, periodToRegister);

    // Transaction 1: lock, validate, and add the initial balance movement when the month is new.
    final boolean initialMovementAdded =
        unitOfWork.executeInTransaction(
            () -> addInitialMovementWhenMissing(userId, accountId, addMonthlyBalanceCommand));

    // The adjust of the initial movement needs the product lock, so wait outside any transaction.
    if (initialMovementAdded) {
      processMonthlyBalanceService.awaitPendingSync();
    }

    // Transaction 2: lock again and read fresh data before writing the report.
    final MonthlyBalanceDTO monthlyBalanceDTO =
        unitOfWork.executeInTransaction(
            () -> registerReport(userId, accountId, addMonthlyBalanceCommand));

    log.info(
        "Monthly Balance registration completed successfully for productDTO:{} and period: {}",
        accountId,
        periodToRegister);

    return monthlyBalanceDTO;
  }

  private boolean addInitialMovementWhenMissing(
      final UUID userId, final ProductId accountId, final AddMonthlyBalanceCommand command)
      throws BusinessException {
    accountService.lockForUpdate(accountId);

    final YearMonth periodToRegister = command.monthlyPeriod();
    validateConsecutiveMonthlyBalances(accountId, periodToRegister);

    if (findMonthlyBalanceByPeriod(accountId, periodToRegister) != null) {
      return false;
    }

    // If it's the first monthly balance, create a movement and it will create the monthly balance
    log.info("The monthly balanced has not been reported before. Creating Initial Balance Movement");
    final AddMovementCommand movementCommand =
        new AddMovementCommand(
            periodToRegister.atDay(1),
            command.closingBalance(),
            null,
            MovementType.DEPOSIT,
            incomeInitialBalanceCat,
            null);

    accountMovementService.addMovementToProduct(new ProductPK(userId, accountId), movementCommand);
    return true;
  }

  private MonthlyBalanceDTO registerReport(
      final UUID userId, final ProductId accountId, final AddMonthlyBalanceCommand command)
      throws BusinessException {
    accountService.lockForUpdate(accountId);

    final YearMonth periodToRegister = command.monthlyPeriod();
    validateConsecutiveMonthlyBalances(accountId, periodToRegister);

    final MonthlyBalanceDomain monthlyBalanceDomain =
        findMonthlyBalanceByPeriod(accountId, periodToRegister);
    if (monthlyBalanceDomain == null) {
      throw new IllegalArgumentException("Monthly Balance not found");
    }

    return findProfitStrategy(command)
        .registerOfficialMonthlyBalance(
            new ProductPK(userId, accountId),
            MonthlyBalanceMapper.toDTO(monthlyBalanceDomain),
            command);
  }

  private void validatePeriod(final LocalDate runningDate, final YearMonth periodToRegister)
      throws BusinessException {
    if (!periodToRegister.isBefore(YearMonth.from(runningDate))) {
      throw new BusinessException(
          BusinessApplicationExceptionType.MONTHLY_BALANCE_PERIOD_NOT_IN_PAST);
    }
  }

  private void validateConsecutiveMonthlyBalances(
      final ProductId accountId, final YearMonth periodToRegister) throws BusinessException {

    log.debug("Validating consecutive balances for period {}", periodToRegister);

    final Optional<MonthlyBalanceDTO> lastOfficialReport =
        monthlyBalanceService.findLastOfficialReport(accountId);
    if (lastOfficialReport.isPresent()) {
      final YearMonth lastOfficialReportPeriod = lastOfficialReport.get().period();

      if (!periodToRegister.equals(lastOfficialReportPeriod.plusMonths(1))) {
        throw new BusinessException(
            BusinessApplicationExceptionType.MONTHLY_BALANCE_NOT_CONSECUTIVE,
            lastOfficialReportPeriod);
      }
    }
  }

  private MonthlyBalanceDomain findMonthlyBalanceByPeriod(
      final ProductId accountId, final YearMonth periodToRegister) {
    return monthlyBalanceService
        .findByAccountIdYearAndMonth(
            accountId, periodToRegister.getYear(), periodToRegister.getMonthValue())
        .map(MonthlyBalanceMapper::toDomain)
        .orElse(null);
  }

  private MonthlyBalanceProfitStrategy findProfitStrategy(final AddMonthlyBalanceCommand command) {
    if (command.monthlyProfitReported() != null) {
      return new ReportedProfitStrategy(
          monthlyBalanceService, accountService, accountMovementService);
    }
    return new UnreportedProfitStrategy(monthlyBalanceService, accountService);
  }
}

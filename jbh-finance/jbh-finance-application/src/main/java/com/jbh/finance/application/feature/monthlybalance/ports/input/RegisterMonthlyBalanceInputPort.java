package com.jbh.finance.application.feature.monthlybalance.ports.input;

import com.jbh.commons.exception.BusinessException;
import com.jbh.finance.application.feature.monthlybalance.commands.AddMonthlyBalanceCommand;
import com.jbh.finance.application.feature.monthlybalance.dto.MonthlyBalanceDTO;
import com.jbh.finance.application.feature.monthlybalance.mappers.MonthlyBalanceMapper;
import com.jbh.finance.application.feature.monthlybalance.services.MonthlyBalanceProfitStrategy;
import com.jbh.finance.application.feature.monthlybalance.services.MonthlyBalanceService;
import com.jbh.finance.application.feature.monthlybalance.services.ReportedProfitStrategy;
import com.jbh.finance.application.feature.monthlybalance.services.UnreportedProfitStrategy;
import com.jbh.finance.application.feature.monthlybalance.usecases.RegisterMonthlyBalanceUseCase;
import com.jbh.finance.application.feature.movement.commands.AddMovementCommand;
import com.jbh.finance.application.feature.movement.services.AccountMovementApplicationService;
import com.jbh.finance.application.feature.product.services.ProductsService;
import com.jbh.finance.application.shared.exceptions.BusinessApplicationExceptionType;
import com.jbh.finance.domain.monthlybalance.MonthlyBalanceDomain;
import com.jbh.finance.domain.movement.vo.IncomeCategory;
import com.jbh.finance.domain.movement.vo.MovementCategoryVO;
import com.jbh.finance.domain.movement.vo.MovementType;
import com.jbh.finance.domain.product.vo.ProductId;
import com.jbh.finance.domain.product.vo.ProductPK;
import java.time.Duration;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.Optional;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RegisterMonthlyBalanceInputPort implements RegisterMonthlyBalanceUseCase {

  private static final Logger log = LoggerFactory.getLogger(RegisterMonthlyBalanceInputPort.class);

  private final MonthlyBalanceService monthlyBalanceService;
  private final ProductsService accountService;
  private final AccountMovementApplicationService accountMovementService;

  public RegisterMonthlyBalanceInputPort(
      final MonthlyBalanceService monthlyBalanceService,
      final ProductsService accountService,
      final AccountMovementApplicationService accountMovementService) {
    this.accountMovementService = accountMovementService;
    this.accountService = accountService;
    this.monthlyBalanceService = monthlyBalanceService;
  }

  /**
   * It worth suggest calling this use case once the next month has started with the profit
   * reported. The user registers a monthly balance once the month has ended. \n It isn't associated
   * with any category. \n The monthly balance can already exist with some movements associated. \n
   * The monthly profit is provided by the institution account. \n Based on the monthly balances,
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
        "Registering Official Monthly Balance for account {} on {}", accountId, periodToRegister);

    validatePeriod(runningDate, periodToRegister);

    validateConsecutiveMonthlyBalances(accountId, periodToRegister);

    // FIXME TODO - Move all this logic to the service itself
    MonthlyBalanceDomain monthlyBalanceDomain =
        findMonthlyBalanceByPeriod(accountId, periodToRegister);

    // If it's the first monthly balance, create a movement and it will create the monthly balance
    if (monthlyBalanceDomain == null) {
      log.info(
          "The monthly balanced has not been reported before. Creating Initial Balance Movement");
      final AddMovementCommand movementCommand =
          new AddMovementCommand(
              addMonthlyBalanceCommand.monthlyPeriod().atDay(1),
              addMonthlyBalanceCommand.closingBalance(),
              null,
              MovementType.DEPOSIT,
              MovementCategoryVO.withType(IncomeCategory.INITIAL_BALANCE),
              null);

      accountMovementService.addMovementProcessingBalances(
          new ProductPK(userId, accountId), movementCommand);

      try {
        // FIXME TODO - This is a hack to wait for the async task to be executed and the report is
        // created
        Thread.sleep(Duration.ofSeconds(2).toMillis());
      } catch (final InterruptedException e) {
        log.error("Error waiting for the async task to be executed", e);
      }
      log.info("The monthly balance should be created by the movement. Finding it again");
      monthlyBalanceDomain = findMonthlyBalanceByPeriod(accountId, periodToRegister);
    }

    if (monthlyBalanceDomain == null) {
      throw new IllegalArgumentException("Monthly Balance not found");
    }
    final ProductPK accountPK = new ProductPK(userId, accountId);
    final MonthlyBalanceDTO monthlyBalanceDTO =
        findProfitStrategy(addMonthlyBalanceCommand)
            .registerOfficialMonthlyBalance(
                accountPK,
                MonthlyBalanceMapper.toDTO(monthlyBalanceDomain),
                addMonthlyBalanceCommand);

    log.info(
        "Monthly Balance registration completed successfully for account:{} and period: {}",
        accountId,
        periodToRegister);

    return monthlyBalanceDTO;
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

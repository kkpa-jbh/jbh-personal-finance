package com.jbh.account.application.core.ports.input;

import com.jbh.account.application.core.dto.MonthlyBalanceDTO;
import com.jbh.account.application.core.mappers.MonthlyBalanceMapper;
import com.jbh.account.application.core.services.account.AccountService;
import com.jbh.account.application.core.services.monthlybalance.MonthlyBalanceProfitStrategy;
import com.jbh.account.application.core.services.monthlybalance.MonthlyBalanceService;
import com.jbh.account.application.core.services.monthlybalance.ReportedProfitStrategy;
import com.jbh.account.application.core.services.monthlybalance.UnreportedProfitStrategy;
import com.jbh.account.application.core.services.movements.AccountMovementApplicationService;
import com.jbh.account.application.core.usecases.RegisterMonthlyBalanceUseCase;
import com.jbh.account.application.core.vo.commands.AddMonthlyBalanceCommand;
import com.jbh.account.application.core.vo.commands.AddMovementCommand;
import com.jbh.account.domain.entity.MonthlyBalanceDomain;
import com.jbh.account.domain.exceptions.AccountBusinessException;
import com.jbh.account.domain.exceptions.JbhExceptionMessage;
import com.jbh.account.domain.vo.AccountId;
import com.jbh.account.domain.vo.AccountPK;
import com.jbh.account.domain.vo.IncomeCategory;
import com.jbh.account.domain.vo.MovementCategoryDTO;
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
  private final AccountService accountService;
  private final AccountMovementApplicationService accountMovementService;

  public RegisterMonthlyBalanceInputPort(
      final MonthlyBalanceService monthlyBalanceService,
      final AccountService accountService,
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
   * @throws AccountBusinessException
   */
  @Override
  public MonthlyBalanceDTO registerOfficialMonthlyBalance(
      final LocalDate runningDate,
      final UUID userId,
      final AccountId accountId,
      final AddMonthlyBalanceCommand addMonthlyBalanceCommand)
      throws AccountBusinessException {

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
      accountMovementService.addMovementProcessingBalances(
          new AccountPK(userId, accountId),
          new AddMovementCommand(
              addMonthlyBalanceCommand.monthlyPeriod().atDay(1),
              addMonthlyBalanceCommand.closingBalance(),
              MovementCategoryDTO.withType(IncomeCategory.INITIAL_BALANCE)));

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
    final AccountPK accountPK = new AccountPK(userId, accountId);
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
      throws AccountBusinessException {
    if (!periodToRegister.isBefore(YearMonth.from(runningDate))) {
      throw new AccountBusinessException(
          "The monthly balance period is not in the past",
          new JbhExceptionMessage(
              "The monthly balance period is not in the past",
              "El periodo de la cuenta no es en el pasado"));
    }
  }

  private void validateConsecutiveMonthlyBalances(
      final AccountId accountId, final YearMonth periodToRegister) throws AccountBusinessException {

    log.debug("Validating consecutive balances for period {}", periodToRegister);

    final Optional<MonthlyBalanceDTO> lastOfficialReport =
        monthlyBalanceService.findLastOfficialReport(accountId);
    if (lastOfficialReport.isPresent()) {
      final YearMonth lastOfficialReportPeriod = lastOfficialReport.get().period();

      if (!periodToRegister.equals(lastOfficialReportPeriod.plusMonths(1))) {
        throw new AccountBusinessException(
            "The monthly balance period is not consecutive. The last period was: "
                + lastOfficialReportPeriod,
            new JbhExceptionMessage(
                "The monthly balance period is not consecutive. The last period was: "
                    + lastOfficialReportPeriod,
                "El periodo de la cuenta no es consecutivo. La última periodo fue: "
                    + lastOfficialReportPeriod));
      }
    }
  }

  private MonthlyBalanceDomain findMonthlyBalanceByPeriod(
      final AccountId accountId, final YearMonth periodToRegister) {
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

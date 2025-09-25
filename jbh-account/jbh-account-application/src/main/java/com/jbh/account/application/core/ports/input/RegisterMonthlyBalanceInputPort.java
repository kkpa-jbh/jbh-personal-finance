package com.jbh.account.application.core.ports.input;

import static com.jbh.account.application.core.mappers.MonthlyBalanceMapper.toDTO;

import com.jbh.account.application.core.dto.MonthlyBalanceDTO;
import com.jbh.account.application.core.mappers.MonthlyBalanceMapper;
import com.jbh.account.application.core.services.account.AccountService;
import com.jbh.account.application.core.services.monthlybalance.MonthlyBalanceService;
import com.jbh.account.application.core.services.movements.AccountMovementService;
import com.jbh.account.application.core.usecases.RegisterMonthlyBalanceUseCase;
import com.jbh.account.application.core.vo.commands.AddMonthlyBalanceCommand;
import com.jbh.account.application.exceptions.JbhSpecificationApplication;
import com.jbh.account.domain.entity.AccountMonthlyBalanceDomain;
import com.jbh.account.domain.utils.JbhStringUtils;
import com.jbh.account.domain.vo.AccountId;
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
  private final AccountMovementService accountMovementService;

  public RegisterMonthlyBalanceInputPort(
      final MonthlyBalanceService monthlyBalanceService,
      final AccountService accountService,
      final AccountMovementService accountMovementService) {
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
   * @param command
   * @return
   * @throws JbhSpecificationApplication
   */
  @Override
  public MonthlyBalanceDTO registerOfficialMonthlyBalance(
      final LocalDate runningDate,
      final UUID userId,
      final AccountId accountId,
      final AddMonthlyBalanceCommand command)
      throws JbhSpecificationApplication {

    // Command validation
    command.validate();

    final YearMonth periodToRegister = command.monthlyPeriod();
    log.info(
        "Registering Official Monthly Balance for account {} on {}", accountId, periodToRegister);

    validatePeriod(runningDate, periodToRegister);

    validateConsecutiveMonthlyBalances(accountId, periodToRegister);

    // FIXME TODO - Move all this logic to the service itself
    final AccountMonthlyBalanceDomain monthlyBalanceDomain =
        monthlyBalanceService
            .findByAccountIdYearAndMonth(
                accountId, periodToRegister.getYear(), periodToRegister.getMonthValue())
            .map(MonthlyBalanceMapper::toDomain)
            .orElseGet(() -> AccountMonthlyBalanceDomain.withPeriod(accountId, periodToRegister));

    monthlyBalanceDomain.assignOfficialMonthlyReport(
        command.closingBalance(), command.monthlyProfitReported());

    final MonthlyBalanceDTO monthlyBalanceDTO = toDTO(monthlyBalanceDomain);
    monthlyBalanceService.saveBalance(monthlyBalanceDTO);

    if (monthlyBalanceService.isLastOfficialReport(monthlyBalanceDTO)) {
      accountService.syncByMonthlyReport(monthlyBalanceDTO);
    }

    monthlyBalanceService.updateOpeningBalanceNextMonth(monthlyBalanceDTO);

    accountMovementService.addDividendsMovement(monthlyBalanceDTO);

    log.info(
        "Monthly Balance registration completed successfully for account:{} and period: {}",
        accountId,
        periodToRegister);

    return monthlyBalanceDTO;
  }

  private void validatePeriod(final LocalDate runningDate, final YearMonth periodToRegister)
      throws JbhSpecificationApplication {
    if (!periodToRegister.isBefore(YearMonth.from(runningDate))) {
      throw new JbhSpecificationApplication(
          "The monthly balance period is not in the past",
          null,
          "The monthly balance period is not in the past");
    }
  }

  private void validateConsecutiveMonthlyBalances(
      final AccountId accountId, final YearMonth periodToRegister)
      throws JbhSpecificationApplication {

    log.info("Validating consecutive balances for period {}", periodToRegister);

    final Optional<MonthlyBalanceDTO> lastOfficialReport =
        monthlyBalanceService.findLastOfficialReport(accountId);
    if (lastOfficialReport.isPresent()) {
      final YearMonth lastOfficialReportPeriod = lastOfficialReport.get().period();

      if (!periodToRegister.equals(lastOfficialReportPeriod.plusMonths(1))) {
        throw new JbhSpecificationApplication(
            "The monthly balance period is not consecutive. The last period was: "
                + lastOfficialReportPeriod,
            null,
            JbhStringUtils.buildJsonMessage(
                "The monthly balance period is not consecutive.",
                "El periodo de la cuenta no es consecutivo. La última periodo fue: "
                    + lastOfficialReportPeriod));
      }
    }
  }
}

package com.jbh.account.application.accounts.ports.input;

import com.jbh.account.application.accounts.ports.output.AccountRepository;
import com.jbh.account.application.accounts.services.monthlybalance.MonthlyBalanceService;
import com.jbh.account.application.accounts.usecases.RegisterMonthlyBalanceUseCase;
import com.jbh.account.application.accounts.vo.commands.AddMonthlyBalanceCommand;
import com.jbh.account.application.exceptions.JbhSpecificationApplication;
import com.jbh.account.application.movements.ports.output.AccountMovementRepository;
import com.jbh.account.domain.entity.AccountMonthlyBalanceDomain;
import com.jbh.account.domain.vo.AccountId;
import com.jbh.account.domain.vo.AccountMonthlyBalanceDTO;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RegisterMonthlyBalanceInputPort implements RegisterMonthlyBalanceUseCase {

  private static final Logger log = LoggerFactory.getLogger(RegisterMonthlyBalanceInputPort.class);

  private final AccountRepository accountRepo;
  private final AccountMovementRepository accountMovementRepository;
  private final MonthlyBalanceService monthlyBalanceService;

  public RegisterMonthlyBalanceInputPort(
      final AccountRepository accountRepo,
      final MonthlyBalanceService monthlyBalanceService,
      final AccountMovementRepository accountMovementRepository) {
    this.accountRepo = accountRepo;
    this.monthlyBalanceService = monthlyBalanceService;
    this.accountMovementRepository = accountMovementRepository;
  }

  /**
   * The user registers a monthly balance once the month has ended. \n It isn't associated with any
   * category. \n The monthly balance can already exist with some movements associated. \n The
   * monthly profit is provided by the institution account. \n Based on the monthly profit average
   * of the year, this method will calculate the estimatedAnnualYield for the month. This method
   * will also calculate the monthlyExpenses for the month. After syncing the monthly balance, it
   * should update the opening balance of the next month.
   *
   * @param runningDate
   * @param userId
   * @param accountId
   * @param command
   * @return
   * @throws JbhSpecificationApplication
   */
  @Override
  public AccountMonthlyBalanceDTO registerMonthlyBalance(
      final LocalDate runningDate,
      final UUID userId,
      final AccountId accountId,
      final AddMonthlyBalanceCommand command)
      throws JbhSpecificationApplication {

    command.validate();
    final YearMonth periodToRegister = command.monthlyPeriod();

    log.info(
        "Registering Official Monthly Balance for account {} on {}", accountId, periodToRegister);

    if (!periodToRegister.isBefore(YearMonth.from(runningDate))) {
      throw new JbhSpecificationApplication(
          "The monthly balance period is not in the past",
          null,
          "The monthly balance period is not in the past");
    }

    final AccountMonthlyBalanceDTO monthlyBalanceDTO =
        monthlyBalanceService
            .findByAccountIdYearAndMonth(
                accountId, periodToRegister.getYear(), periodToRegister.getMonthValue())
            .orElseGet(
                () -> {
                  return AccountMonthlyBalanceDomain.withPeriod(
                          accountId, periodToRegister.getYear(), periodToRegister.getMonthValue())
                      .toDTO();
                });

    monthlyBalanceDTO.syncOfficialMonthlyReport(
        command.closingBalance(), command.monthlyProfitReported());

    monthlyBalanceService.saveBalance(monthlyBalanceDTO);
    monthlyBalanceService.updateOpeningBalanceNextMonth(monthlyBalanceDTO);

    log.info(
        "Monthly Balance registration completed successfully for account:{} and period: {}",
        accountId,
        periodToRegister);

    return monthlyBalanceDTO;
  }
}

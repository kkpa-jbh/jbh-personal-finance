package com.jbh.account.application.core.services.movements;

import static com.jbh.account.domain.utils.JbhMoneyUtils.withJBHDecimals;
import static com.jbh.account.domain.vo.MovementType.WITHDRAWAL;

import com.jbh.account.application.acid.UnitOfWork;
import com.jbh.account.application.core.dto.AccountDTO;
import com.jbh.account.application.core.dto.AddBasicMovementDTO;
import com.jbh.account.application.core.dto.MonthlyBalanceDTO;
import com.jbh.account.application.core.dto.MovementDTO;
import com.jbh.account.application.core.mappers.MovementMapper;
import com.jbh.account.application.core.services.account.AccountService;
import com.jbh.account.application.core.services.monthlybalance.MonthlyBalanceService;
import com.jbh.account.application.core.validation.accounttype.AccountMovementValidatorFactory;
import com.jbh.account.application.core.vo.commands.AddMonthlyBalanceCommand;
import com.jbh.account.application.core.vo.commands.AddMovementCommand;
import com.jbh.account.domain.exceptions.AccountBusinessException;
import com.jbh.account.domain.vo.AccountId;
import com.jbh.account.domain.vo.AccountMovementMetadata;
import com.jbh.account.domain.vo.AccountPK;
import com.jbh.account.domain.vo.ExpenseCategory;
import com.jbh.account.domain.vo.IncomeCategory;
import com.jbh.account.domain.vo.MovementCategoryDTO;
import com.jbh.account.domain.vo.MovementType;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.Optional;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AccountMovementApplicationServiceImpl implements AccountMovementApplicationService {
  private static final Logger log =
      LoggerFactory.getLogger(AccountMovementApplicationServiceImpl.class);

  private final AccountMovementService accountMovementService;
  private final AccountService accountService;
  private final MonthlyBalanceService monthlyBalanceService;
  private final UnitOfWork unitOfWork;

  private final AccountMovementValidatorFactory movementValidatorFactory;

  public AccountMovementApplicationServiceImpl(
      final AccountMovementService accountMovementService,
      final AccountService accountService,
      final MonthlyBalanceService monthlyBalanceService,
      final UnitOfWork unitOfWork) {
    this.unitOfWork = unitOfWork;
    this.accountService = accountService;
    this.monthlyBalanceService = monthlyBalanceService;
    this.accountMovementService = accountMovementService;

    movementValidatorFactory = new AccountMovementValidatorFactory(accountMovementService);
  }

  // TODO: Move this out of the service. This service should be responsible of
  // adding the dividends generically
  @Override
  public void addDividendsMovementForNextMonth(
      final AccountPK accountPK, final AddMonthlyBalanceCommand nextMonthlyBalanceCommand)
      throws AccountBusinessException {
    if (nextMonthlyBalanceCommand == null) {
      log.warn("No monthly balance to add dividends movement");
      return;
    }

    final var accountId = accountPK.accountId();
    final var period = nextMonthlyBalanceCommand.monthlyPeriod().plusMonths(1).atDay(1);
    final var monthlyProfitReported =
        withJBHDecimals(nextMonthlyBalanceCommand.monthlyProfitReported());
    final var incomeWithholdingTaxAmount = nextMonthlyBalanceCommand.incomeWithholdingTaxAmount();
    final var nextMonthBalance = nextMonthlyBalanceCommand.closingBalance();

    if (incomeWithholdingTaxAmount != null && monthlyProfitReported == null) {
      throw new IllegalArgumentException("Monthly profit reported cannot be null");
    }

    if (monthlyProfitReported != null) {
      log.info(
          "Adding {} dividends movement for account {} and period {} with balance snapshot {}",
          monthlyProfitReported,
          accountId,
          period,
          nextMonthBalance);

      // I decided to put the balance snapshot, to make it real with the current balance of the
      // month
      // taking into account the dividends. This balance snapshot should be the same of the account
      // balance.

      /*
      final AddMovementCommand dividendsMovement =
          new AddMovementCommand(
              period,
              monthlyProfitReported,
              nextMonthBalance,
              MovementType.DEPOSIT,
              MovementCategoryDTO.withType(IncomeCategory.DIVIDENDS));

              if (incomeWithholdingTaxAmount != null) {
        final var incomeWithholdingTaxMovement =
            new AddMovementCommand(
                period,
                incomeWithholdingTaxAmount,
                nextMonthBalance,
                WITHDRAWAL,
                MovementCategoryDTO.withType(ExpenseCategory.RETEFUENTE));

        addMovementProcessingBalances(accountPK, incomeWithholdingTaxMovement);

        log.info(
            "Income withholding tax movement {} added successfully for account {} and period {}",
            incomeWithholdingTaxAmount,
            accountId,
            period);
      }

       */

      addDividendsMovement(
          accountPK,
          period,
          monthlyProfitReported,
          nextMonthBalance,
          incomeWithholdingTaxAmount,
          AccountMovementMetadata.createEmpty());
    }
  }

  @Override
  public AddBasicMovementDTO addMovementProcessingBalances(
      final AccountPK accountPK, final AddMovementCommand movementCommand)
      throws AccountBusinessException {
    // Input validations
    movementCommand.validate();

    final var accountId = accountPK.accountId();

    log.info(
        "Analyzing Movement {} for account: {}, {}",
        movementCommand.movementType(),
        accountId.value(),
        movementCommand);

    final YearMonth movementPeriod = YearMonth.from(movementCommand.entryDate());
    final boolean isMonthOfficiallyReported =
        findIfMonthlyBalanceWasOfficialReported(accountId, movementPeriod);

    // Create MovementDTO from command
    final var movementDTO = MovementMapper.fromCommand(accountId, movementCommand);

    return processMovement(movementDTO, accountPK, isMonthOfficiallyReported);
  }

  @Override
  public AddBasicMovementDTO processMovement(
      final MovementDTO movementDTO,
      final AccountPK accountPK,
      final boolean isMonthOfficiallyReported)
      throws AccountBusinessException {
    // Validations
    monthlyBalanceService.validateNewMovementForOfficialMonthlyReport(movementDTO);

    final UUID userId = accountPK.userId();
    final AccountId accountId = accountPK.accountId();

    final AccountDTO syncedAccountDTO =
        accountService.syncByMovement(
            new AccountPK(userId, accountId), movementDTO, isMonthOfficiallyReported);

    validateMovementByAccountType(syncedAccountDTO, movementDTO);

    unitOfWork.execute(
        () -> {
          log.info("ACID operations...");
          log.info("Persisting Movement {} ", movementDTO.movementDate());
          persistMovementDTO(movementDTO);
          accountService.save(syncedAccountDTO);
          log.info("Movement and Account {} persisted successfully", syncedAccountDTO.name());
        });

    log.info("Syncing Monthly Balance for new movement {}", movementDTO);
    MonthlyBalanceDTO monthlyBalanceDTO = null;
    if (syncedAccountDTO.productTypeShouldUpdateMonthlyBalance()) {
      monthlyBalanceDTO = monthlyBalanceService.syncForNewMovement(movementDTO);
    }

    return new AddBasicMovementDTO(syncedAccountDTO, movementDTO, monthlyBalanceDTO);
  }

  @Override
  public void addDividendsMovement(
      final AccountPK accountPK,
      final LocalDate movementDate,
      final BigDecimal dividendsAmount,
      final BigDecimal balanceSnapshot,
      final BigDecimal incomeWithholdingTaxAmount,
      final AccountMovementMetadata metadata)
      throws AccountBusinessException {
    final AddMovementCommand dividendsMovement =
        new AddMovementCommand(
            movementDate,
            dividendsAmount,
            balanceSnapshot,
            MovementType.DEPOSIT,
            MovementCategoryDTO.withType(IncomeCategory.DIVIDENDS));

    addMovementProcessingBalances(accountPK, dividendsMovement);

    log.info(
        "Dividends movement {} added successfully for account {} and period {}",
        dividendsAmount,
        accountPK.accountId(),
        movementDate);

    if (incomeWithholdingTaxAmount != null) {
      final var incomeWithholdingTaxMovement =
          new AddMovementCommand(
              movementDate,
              incomeWithholdingTaxAmount,
              balanceSnapshot,
              WITHDRAWAL,
              MovementCategoryDTO.withType(ExpenseCategory.RETEFUENTE));

      addMovementProcessingBalances(accountPK, incomeWithholdingTaxMovement);

      log.info(
          "Income withholding tax movement {} added successfully for account {} and period {}",
          incomeWithholdingTaxAmount,
          accountPK.accountId(),
          movementDate);
    }
  }

  /**
   * Finds if the monthly balance associated with the movement date was already reported officially.
   *
   * @param accountId
   * @param movementPeriod
   * @return true if the monthly balance was already reported, false otherwise
   */
  private boolean findIfMonthlyBalanceWasOfficialReported(
      final AccountId accountId, final YearMonth movementPeriod) {
    final Optional<MonthlyBalanceDTO> existingMonthlyBalanceOpt =
        monthlyBalanceService.findByAccountIdAndPeriod(accountId, movementPeriod);
    return existingMonthlyBalanceOpt.map(MonthlyBalanceDTO::officialMonthlyReport).orElse(false);
  }

  private void validateMovementByAccountType(
      final AccountDTO existingAccount, final MovementDTO movementDTO)
      throws AccountBusinessException {
    movementValidatorFactory
        .getValidator(existingAccount.type())
        .validateMovementByAccountType(existingAccount, movementDTO);
  }

  private void persistMovementDTO(final MovementDTO movementDTO) {
    accountMovementService.save(movementDTO);
  }
}

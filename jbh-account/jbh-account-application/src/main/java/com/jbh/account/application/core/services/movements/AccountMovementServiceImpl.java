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
import com.jbh.account.application.core.vo.commands.AddMonthlyBalanceCommand;
import com.jbh.account.application.core.vo.commands.AddMovementCommand;
import com.jbh.account.application.movements.ports.output.AccountMovementRepository;
import com.jbh.account.domain.entity.AccountMovementDomain;
import com.jbh.account.domain.entity.MovementCategoryDomain;
import com.jbh.account.domain.exceptions.AccountBusinessException;
import com.jbh.account.domain.vo.AccountId;
import com.jbh.account.domain.vo.AccountPK;
import com.jbh.account.domain.vo.ExpenseCategory;
import com.jbh.account.domain.vo.IncomeCategory;
import com.jbh.account.domain.vo.MovementCategoryDTO;
import com.jbh.account.domain.vo.MovementType;
import java.math.BigDecimal;
import java.time.YearMonth;
import java.util.HashMap;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AccountMovementServiceImpl implements AccountMovementService {
  private static final Logger log = LoggerFactory.getLogger(AccountMovementServiceImpl.class);
  private final AccountMovementRepository movementRepo;
  private final AccountService accountService;
  private final MonthlyBalanceService monthlyBalanceService;
  private final UnitOfWork unitOfWork;

  public AccountMovementServiceImpl(
      final AccountMovementRepository movementRepo,
      final AccountService accountService,
      final MonthlyBalanceService monthlyBalanceService,
      final UnitOfWork unitOfWork) {
    this.unitOfWork = unitOfWork;
    this.accountService = accountService;
    this.monthlyBalanceService = monthlyBalanceService;
    this.movementRepo = movementRepo;
  }

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

      final AddMovementCommand dividendsMovement =
          new AddMovementCommand(
              period,
              monthlyProfitReported,
              nextMonthBalance,
              MovementType.DEPOSIT,
              MovementCategoryDTO.withType(IncomeCategory.DIVIDENDS));

      addMovementProcessingBalances(accountPK, dividendsMovement);

      log.info(
          "Dividends movement {} added successfully for account {} and period {}",
          monthlyProfitReported,
          accountId,
          period);

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
    }
  }

  @Override
  public AddBasicMovementDTO addMovementProcessingBalances(
      final AccountPK accountPK, final AddMovementCommand movementCommand)
      throws AccountBusinessException {
    // Input validations
    movementCommand.validate();

    final var userId = accountPK.userId();
    final var accountId = accountPK.accountId();

    final var movementBalanceSnapshot = movementCommand.balanceSnapshot();
    log.info(
        "Analyzing Movement {} for account: {}, date:{} category:{} amount: {} snapshot: {}",
        movementCommand.movementType(),
        accountId.value(),
        movementCommand.entryDate(),
        movementCommand.categoryDTO(),
        movementCommand.totalAmount(),
        movementBalanceSnapshot);

    final YearMonth movementPeriod = YearMonth.from(movementCommand.entryDate());
    final boolean isMonthOfficiallyReported =
        findIfMonthlyBalanceWasOfficialReported(accountId, movementPeriod);

    // Get Movement Type and Movement Amount
    BigDecimal totalAmount = movementCommand.totalAmount();
    final MovementType movementType = movementCommand.movementType();
    totalAmount = movementType == WITHDRAWAL ? totalAmount.negate() : totalAmount;
    final AccountMovementDomain newMovement =
        new AccountMovementDomain(
            accountId,
            movementType,
            movementCommand.entryDate(),
            totalAmount,
            movementBalanceSnapshot,
            new HashMap<>(),
            MovementCategoryDomain.withDTO(movementCommand.categoryDTO()));
    final var movementDTO = MovementMapper.toDTO(newMovement);

    // Validations
    monthlyBalanceService.validateNewMovement(movementDTO);

    // Then

    final AccountDTO accountDTO =
        accountService.syncByMovement(
            new AccountPK(userId, accountId), movementDTO, isMonthOfficiallyReported);
    log.info("Account {} was synced by Movement.." + accountDTO.name(), movementDTO);

    unitOfWork.execute(
        () -> {
          log.info("ACID operations...");
          log.info("Persisting Movement {} ", movementDTO.movementDate());
          persistMovementDTO(movementDTO);
          accountService.save(accountDTO);
          log.info("Movement and Account {} persisted successfully", accountDTO.name());
        });

    log.info("Syncing Monthly Balance for new movement {}", movementDTO);
    final MonthlyBalanceDTO monthlyBalanceDTO =
        monthlyBalanceService.syncForNewMovement(movementDTO);

    return new AddBasicMovementDTO(accountDTO, movementDTO, monthlyBalanceDTO);
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

  private void persistMovementDTO(final MovementDTO movementDTO) {
    movementRepo.save(movementDTO);
  }
}

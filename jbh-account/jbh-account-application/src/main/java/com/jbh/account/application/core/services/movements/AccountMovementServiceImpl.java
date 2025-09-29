package com.jbh.account.application.core.services.movements;

import static com.jbh.account.domain.utils.MoneyUtils.withJBHDecimals;
import static com.jbh.account.domain.vo.MovementType.WITHDRAWAL;

import com.jbh.account.application.acid.UnitOfWork;
import com.jbh.account.application.core.dto.AccountDTO;
import com.jbh.account.application.core.dto.AddBasicMovementDTO;
import com.jbh.account.application.core.dto.MonthlyBalanceDTO;
import com.jbh.account.application.core.dto.MovementDTO;
import com.jbh.account.application.core.mappers.MovementMapper;
import com.jbh.account.application.core.services.account.AccountService;
import com.jbh.account.application.core.services.monthlybalance.MonthlyBalanceService;
import com.jbh.account.application.core.vo.commands.AddMovementCommand;
import com.jbh.account.application.exceptions.JbhSpecificationApplication;
import com.jbh.account.application.movements.ports.output.AccountMovementRepository;
import com.jbh.account.domain.entity.AccountMovementDomain;
import com.jbh.account.domain.entity.MovementCategoryDomain;
import com.jbh.account.domain.vo.AccountId;
import com.jbh.account.domain.vo.AccountPK;
import com.jbh.account.domain.vo.IncomeCategory;
import com.jbh.account.domain.vo.MovementCategoryDTO;
import com.jbh.account.domain.vo.MovementType;
import java.math.BigDecimal;
import java.time.YearMonth;
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
      final AccountPK accountPK, final MonthlyBalanceDTO monthlyBalanceDTO)
      throws JbhSpecificationApplication {
    if (monthlyBalanceDTO == null) {
      log.warn("No monthly balance to add dividends movement");
      return;
    }

    final var accountId = monthlyBalanceDTO.accountId();
    final var period = monthlyBalanceDTO.period().plusMonths(1).atDay(1);
    final var monthlyProfitReported = withJBHDecimals(monthlyBalanceDTO.monthlyProfitReported());

    if (monthlyProfitReported != null) {
      log.info(
          "Adding {} dividends movement for account {} and period {}",
          monthlyProfitReported,
          accountId,
          period);

      // I decided to put the balance snapshot, to make it real with the current balance of the
      // month
      // taking into account the dividends. This balance snapshot should be the same of the account
      // balance.
      final var nextMonthBalance = monthlyBalanceDTO.closingBalance().add(monthlyProfitReported);
      final AddMovementCommand dividendsMovement =
          new AddMovementCommand(
              period,
              monthlyProfitReported,
              nextMonthBalance,
              MovementType.DEPOSIT,
              MovementCategoryDTO.withType(IncomeCategory.DIVIDENDS));

      addMovement(accountPK, dividendsMovement);

      log.info(
          "Dividends movement added successfully for account {} and period {}", accountId, period);
    }
  }

  @Override
  public AddBasicMovementDTO addMovement(
      final AccountPK accountPK, final AddMovementCommand movementCommand)
      throws JbhSpecificationApplication {
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
        AccountMovementDomain.with(
            accountId,
            movementCommand.entryDate(),
            totalAmount,
            movementBalanceSnapshot,
            movementType,
            MovementCategoryDomain.withDTO(movementCommand.categoryDTO()));
    final var movementDTO = MovementMapper.toDTO(newMovement);

    // Validations
    monthlyBalanceService.validateNewMovement(movementDTO);

    // Then

    final AccountDTO accountDTO =
        accountService.syncByMovement(
            new AccountPK(userId, accountId), movementDTO, isMonthOfficiallyReported);

    unitOfWork.execute(
        () -> {
          log.info(
              "Persisting Movement {} and Account with ACID operation", movementDTO.movementDate());
          persistMovementDTO(movementDTO);
          accountService.save(accountDTO);
          log.info("Movement and Account persisted successfully");
        });

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

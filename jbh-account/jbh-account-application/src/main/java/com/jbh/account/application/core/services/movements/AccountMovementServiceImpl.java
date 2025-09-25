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
  public void addDividendsMovement(
      final AccountPK accountPK, final MonthlyBalanceDTO monthlyBalanceDTO) {
    if (monthlyBalanceDTO == null) {
      log.warn("No monthly balance to add dividends movement");
      return;
    }

    final var accountId = monthlyBalanceDTO.accountId();
    final var period = monthlyBalanceDTO.period();
    final var closingBalance = withJBHDecimals(monthlyBalanceDTO.closingBalance());
    final var monthlyProfitReported = withJBHDecimals(monthlyBalanceDTO.monthlyProfitReported());

    if (monthlyProfitReported != null) {
      log.info(
          "Adding {} dividends movement for account {} and period {}",
          monthlyProfitReported,
          accountId,
          period);

      final AddMovementCommand dividendsMovement =
          new AddMovementCommand(
              period.plusMonths(1).atDay(1),
              monthlyProfitReported,
              closingBalance,
              MovementType.DEPOSIT,
              MovementCategoryDTO.withType(IncomeCategory.DIVIDENDS));

      addMovement(accountPK, dividendsMovement);

      log.info(
          "Dividends movement added successfully for account {} and period {}", accountId, period);
    }
  }

  @Override
  public AddBasicMovementDTO addMovement(
      final AccountPK accountPK, final AddMovementCommand movementCommand) {
    // Input validations
    movementCommand.validate();

    final var userId = accountPK.userId();
    final var accountId = accountPK.accountId();

    log.info(
        "Analyzing Movement {} for account: {}, date:{} category:{} amount: {} snapshot: {}",
        movementCommand.movementType(),
        accountId.value(),
        movementCommand.entryDate(),
        movementCommand.categoryDTO(),
        movementCommand.totalAmount(),
        movementCommand.balanceSnapshot());

    final YearMonth movementPeriod = YearMonth.from(movementCommand.entryDate());

    // Get Movement Type and Movement Amount
    BigDecimal totalAmount = movementCommand.totalAmount();
    final MovementType movementType = movementCommand.movementType();
    totalAmount = movementType == WITHDRAWAL ? totalAmount.negate() : totalAmount;
    final AccountMovementDomain newMovement =
        AccountMovementDomain.with(
            accountId,
            movementCommand.entryDate(),
            totalAmount,
            movementCommand.balanceSnapshot(),
            movementType,
            MovementCategoryDomain.withDTO(movementCommand.categoryDTO()));
    final var movementDTO = MovementMapper.toDTO(newMovement);

    final boolean isMonthOfficiallyReported =
        findIfMonthlyBalanceWasOfficialReported(accountId, movementPeriod);
    final AccountDTO accountDTO =
        accountService.syncByMovement(
            new AccountPK(userId, accountId), movementDTO, isMonthOfficiallyReported);

    unitOfWork.execute(
        () -> {
          log.info(
              "Persisting Movement {} and Account with ACID operation", movementDTO.movementDate());
          persistMovementDTO(movementDTO);
          accountService.save(accountDTO);
        });

    return new AddBasicMovementDTO(accountDTO, movementDTO);
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

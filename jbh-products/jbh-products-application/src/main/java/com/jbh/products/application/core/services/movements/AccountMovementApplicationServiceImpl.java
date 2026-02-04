package com.jbh.products.application.core.services.movements;

import static com.jbh.commons.util.JbhMoneyUtils.withJBHDecimals;
import static com.jbh.products.domain.vo.MovementType.WITHDRAWAL;

import com.jbh.commons.exception.BusinessException;
import com.jbh.products.application.acid.UnitOfWork;
import com.jbh.products.application.core.dto.AddBasicMovementDTO;
import com.jbh.products.application.core.dto.MonthlyBalanceDTO;
import com.jbh.products.application.core.dto.MovementDTO;
import com.jbh.products.application.core.dto.ProductDTO;
import com.jbh.products.application.core.mappers.MovementMapper;
import com.jbh.products.application.core.services.account.ProductsService;
import com.jbh.products.application.core.services.monthlybalance.MonthlyBalanceService;
import com.jbh.products.application.core.validation.product_type.ProductMovementValidatorFactory;
import com.jbh.products.application.core.vo.commands.AddMonthlyBalanceCommand;
import com.jbh.products.application.core.vo.commands.AddMovementCommand;
import com.jbh.products.domain.vo.AccountMovementMetadata;
import com.jbh.products.domain.vo.ExpenseCategory;
import com.jbh.products.domain.vo.IncomeCategory;
import com.jbh.products.domain.vo.MovementCategoryDTO;
import com.jbh.products.domain.vo.MovementType;
import com.jbh.products.domain.vo.ProductId;
import com.jbh.products.domain.vo.ProductPK;
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
  private final ProductsService accountService;
  private final MonthlyBalanceService monthlyBalanceService;
  private final UnitOfWork unitOfWork;

  private final ProductMovementValidatorFactory movementValidatorFactory;

  public AccountMovementApplicationServiceImpl(
      final AccountMovementService accountMovementService,
      final ProductsService accountService,
      final MonthlyBalanceService monthlyBalanceService,
      final UnitOfWork unitOfWork) {
    this.unitOfWork = unitOfWork;
    this.accountService = accountService;
    this.monthlyBalanceService = monthlyBalanceService;
    this.accountMovementService = accountMovementService;

    movementValidatorFactory = new ProductMovementValidatorFactory(accountMovementService);
  }

  // TODO: Move this out of the service. This service should be responsible of
  // adding the dividends generically
  @Override
  public void addDividendsMovementForNextMonth(
      final ProductPK accountPK, final AddMonthlyBalanceCommand nextMonthlyBalanceCommand)
      throws BusinessException {
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
      final ProductPK accountPK, final AddMovementCommand movementCommand)
      throws BusinessException {
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
      final ProductPK accountPK,
      final boolean isMonthOfficiallyReported)
      throws BusinessException {
    // Validations
    monthlyBalanceService.validateNewMovementForOfficialMonthlyReport(movementDTO);

    final UUID userId = accountPK.userId();
    final ProductId accountId = accountPK.accountId();

    final ProductDTO syncedAccountDTO =
        accountService.syncByMovement(
            new ProductPK(userId, accountId), movementDTO, isMonthOfficiallyReported);

    validateMovementByProductType(syncedAccountDTO, movementDTO);

    unitOfWork.execute(
        () -> {
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
      final ProductPK accountPK,
      final LocalDate movementDate,
      final BigDecimal dividendsAmount,
      final BigDecimal balanceSnapshot,
      final BigDecimal incomeWithholdingTaxAmount,
      final AccountMovementMetadata metadata)
      throws BusinessException {
    final AddMovementCommand dividendsMovement =
        new AddMovementCommand(
            movementDate,
            dividendsAmount,
            balanceSnapshot,
            MovementType.DEPOSIT,
            MovementCategoryDTO.withType(IncomeCategory.DIVIDENDS),
            null);

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
              MovementCategoryDTO.withType(ExpenseCategory.RETEFUENTE),
              null);

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
      final ProductId accountId, final YearMonth movementPeriod) {
    final Optional<MonthlyBalanceDTO> existingMonthlyBalanceOpt =
        monthlyBalanceService.findByAccountIdAndPeriod(accountId, movementPeriod);
    return existingMonthlyBalanceOpt.map(MonthlyBalanceDTO::officialMonthlyReport).orElse(false);
  }

  private void validateMovementByProductType(
      final ProductDTO existingAccount, final MovementDTO movementDTO) throws BusinessException {
    movementValidatorFactory
        .getValidator(existingAccount.type())
        .validateMovementByProductType(existingAccount, movementDTO);
  }

  private void persistMovementDTO(final MovementDTO movementDTO) {
    accountMovementService.save(movementDTO);
  }
}

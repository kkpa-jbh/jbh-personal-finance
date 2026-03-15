package com.jbh.finance.application.feature.movement.services;

import static com.jbh.commons.util.JbhMoneyUtils.withJBHDecimals;

import com.jbh.commons.exception.BusinessException;
import com.jbh.commons.exception.GenericSpecificationException;
import com.jbh.commons.util.JbhBooleanUtils;
import com.jbh.finance.application.acid.UnitOfWork;
import com.jbh.finance.application.feature.category.dto.CategoryDTO;
import com.jbh.finance.application.feature.category.services.CategoryService;
import com.jbh.finance.application.feature.monthlybalance.commands.AddMonthlyBalanceCommand;
import com.jbh.finance.application.feature.monthlybalance.dto.MonthlyBalanceDTO;
import com.jbh.finance.application.feature.monthlybalance.services.MonthlyBalanceLifecycleService;
import com.jbh.finance.application.feature.movement.commands.AddMovementCommand;
import com.jbh.finance.application.feature.movement.dto.AddMovementResultDTO;
import com.jbh.finance.application.feature.movement.dto.MovementDTO;
import com.jbh.finance.application.feature.movement.mappers.MovementMapper;
import com.jbh.finance.application.feature.product.dto.ProductDTO;
import com.jbh.finance.application.feature.product.services.ProductLifecycleService;
import com.jbh.finance.application.feature.product.validation.product_type.ProductMovementValidatorFactory;
import com.jbh.finance.domain.movement.vo.MovementMetadata;
import com.jbh.finance.domain.movement.vo.MovementType;
import com.jbh.finance.domain.movement.vo.ProcessMovementOptionsVO;
import com.jbh.finance.domain.product.vo.ProductId;
import com.jbh.finance.domain.product.vo.ProductPK;
import com.jbh.finance.domain.shared.vo.EntityOperationVO;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

// FIXME
@SuppressWarnings("PMD.CouplingBetweenObjects")
public class ProcessMovementServiceImpl implements ProcessMovementService {
  private static final Logger log = LoggerFactory.getLogger(ProcessMovementServiceImpl.class);

  private final MovementLifecycleService movementLifecycleService;
  private final ProductLifecycleService productLifecycleService;
  private final MonthlyBalanceLifecycleService monthlyBalanceService;
  private final UnitOfWork unitOfWork;
  private final CategoryService categoryService;
  private final ProductMovementValidatorFactory movementValidatorFactory;

  private final CategoryDTO incomeDividendsCategory;
  private final CategoryDTO expRetefuenteCat;

  public ProcessMovementServiceImpl(
      final MovementLifecycleService movementLifecycleService,
      final ProductLifecycleService productLifecycleService,
      final MonthlyBalanceLifecycleService monthlyBalanceService,
      final UnitOfWork unitOfWork,
      final CategoryService inputCategoryLifecycleSrv) {
    this.unitOfWork = unitOfWork;
    this.productLifecycleService = productLifecycleService;
    this.monthlyBalanceService = monthlyBalanceService;
    this.movementLifecycleService = movementLifecycleService;
    this.categoryService = inputCategoryLifecycleSrv;
    movementValidatorFactory = new ProductMovementValidatorFactory(movementLifecycleService);

    // System Categories

    incomeDividendsCategory = categoryService.findIncomeDividends();

    expRetefuenteCat = categoryService.findExpenseRetefuente();
  }

  // TODO: Move this out of the service. This service should be responsible of
  // adding the dividends generically
  @Override
  public void addDividendsMovementForNextMonth(
      final ProductPK productPK, final AddMonthlyBalanceCommand nextMonthlyBalanceCommand)
      throws BusinessException {
    if (nextMonthlyBalanceCommand == null) {
      log.warn("No monthly balance to add dividends movement");
      return;
    }

    final var accountId = productPK.productId();
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
          "Adding {} dividends movement for productDTO {} and period {} with balance snapshot {}",
          monthlyProfitReported,
          accountId,
          period,
          nextMonthBalance);

      // I decided to put the balance snapshot, to make it real with the current balance of the
      // month
      // taking into productDTO the dividends. This balance snapshot should be the same of the
      // productDTO
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

        addMovementProcessingBalances(productPK, incomeWithholdingTaxMovement);

        log.info(
            "Income withholding tax movement {} added successfully for productDTO {} and period {}",
            incomeWithholdingTaxAmount,
            productId,
            period);
      }

       */

      addDividendsMovement(
          productPK,
          period,
          monthlyProfitReported,
          nextMonthBalance,
          incomeWithholdingTaxAmount,
          MovementMetadata.createEmpty());
    }
  }

  @Override
  public AddMovementResultDTO addMovementToProduct(
      final ProductPK productPK, final AddMovementCommand movementCommand)
      throws BusinessException {
    // Input validations
    movementCommand.validate();

    final var accountId = productPK.productId();

    log.info(
        "Analyzing Movement {} for productDTO: {}, {}",
        movementCommand.movementType(),
        accountId.value(),
        movementCommand);

    // Create MovementDTO from command
    final var movementDTO = MovementMapper.fromCommand(accountId, movementCommand);

    return processMovement(
        movementDTO, productPK, new ProcessMovementOptionsVO(EntityOperationVO.ADD));
  }

  @Override
  public AddMovementResultDTO liquidateProductByMovement(
      final MovementDTO movementDTO, final ProductPK accountPK) throws BusinessException {

    return processMovement(
        movementDTO, accountPK, new ProcessMovementOptionsVO(EntityOperationVO.ADD));
  }

  @Override
  public void addDividendsMovement(
      final ProductPK accountPK,
      final LocalDate movementDate,
      final BigDecimal dividendsAmount,
      final BigDecimal balanceSnapshot,
      final BigDecimal incomeWithholdingTaxAmount,
      final MovementMetadata metadata)
      throws BusinessException {

    final AddMovementCommand dividendsMovement =
        new AddMovementCommand(
            movementDate,
            dividendsAmount,
            balanceSnapshot,
            MovementType.DEPOSIT,
            incomeDividendsCategory,
            null);

    addMovementToProduct(accountPK, dividendsMovement);

    log.info(
        "Dividends movement {} added successfully for productDTO {} and period {}",
        dividendsAmount,
        accountPK.productId(),
        movementDate);

    if (incomeWithholdingTaxAmount != null) {

      final var incomeWithholdingTaxMovement =
          new AddMovementCommand(
              movementDate,
              incomeWithholdingTaxAmount,
              balanceSnapshot,
              MovementType.WITHDRAWAL,
              expRetefuenteCat,
              null);

      addMovementToProduct(accountPK, incomeWithholdingTaxMovement);

      log.info(
          "Income withholding tax movement {} added successfully for productDTO {} and period {}",
          incomeWithholdingTaxAmount,
          accountPK.productId(),
          movementDate);
    }
  }

  @Override
  public void reverseMovementProcessingBalances(
      final ProductPK productPK, final MovementDTO movement) throws BusinessException {
    processMovement(movement, productPK, new ProcessMovementOptionsVO(EntityOperationVO.REMOVE));
  }

  /**
   * Core movement processing pipeline. Validates, syncs product balance, persists, and updates the
   * monthly balance in a single transactional unit.
   *
   * <p>Steps:
   *
   * <ol>
   *   <li>Validates the movement is allowed for the period (blocks if month is officially closed).
   *   <li>Syncs the product balance via {@link ProductLifecycleService#syncByMovement}.
   *   <li>Validates the movement against the specific product-type rules.
   *   <li>Persists the product balance (always) and the movement (unless {@link
   *       ProcessMovementOptions#movementToReverse()} is {@code true}) inside a {@link UnitOfWork}.
   *   <li>Optionally syncs the monthly balance if the product type requires it.
   * </ol>
   *
   * <p>Called by:
   *
   * <ul>
   *   <li>{@link #addMovementToProduct} – standard entry point for new user-initiated movements.
   *   <li>{@link #liquidateProductByMovement} – used during product liquidation flows.
   * </ul>
   *
   * @param movementDTO the movement to process
   * @param options processing options: owner/product identifier, period report status, and whether
   *     to skip persisting the movement record (e.g. for dry-run or balance-sync-only scenarios)
   * @return result containing the updated product, the movement, and the monthly balance
   * @throws BusinessException if any validation or persistence rule is violated
   */
  private AddMovementResultDTO processMovement(
      final MovementDTO movementDTO,
      final ProductPK productPK,
      final ProcessMovementOptionsVO inputMovOptions)
      throws BusinessException {

    if (inputMovOptions == null) {
      throw new GenericSpecificationException("Process Movement options cannot be null");
    }

    final boolean movementToReverse = inputMovOptions.operation().toRemove();

    // Validates that the movement period is not locked by an official monthly report
    monthlyBalanceService.validateNewMovementForOfficialMonthlyReport(movementDTO);

    final YearMonth movementPeriod = YearMonth.from(movementDTO.movementDate());
    final boolean isMonthOfficiallyReported =
        findIfMonthlyBalanceWasOfficialReported(productPK.productId(), movementPeriod);

    final ProcessMovementOptionsVO movementOptions =
        new ProcessMovementOptionsVO(isMonthOfficiallyReported, inputMovOptions.operation());

    // Recalculates the product balance by applying the movement (does not persist yet)
    final ProductDTO syncedAccountDTO =
        productLifecycleService.syncByMovement(productPK, movementDTO, movementOptions);

    // Applies product-type-specific business rules (e.g. CDT cannot have withdrawals)
    validateMovementByProductType(syncedAccountDTO, movementDTO);

    // Atomically persists the movement and the updated product balance
    unitOfWork.execute(
        () -> {
          if (JbhBooleanUtils.isFalse(movementToReverse)) {
            log.info("Persisting Movement {} ", movementDTO.movementDate());
            persistMovementDTO(movementDTO);
          } else {
            log.info("Reversing Movement {} ", movementDTO.movementDate());
            movementLifecycleService.delete(movementDTO.id().value());
          }

          productLifecycleService.save(syncedAccountDTO);
          log.info("Movement and Account {} persisted successfully", syncedAccountDTO.name());
        });

    // Updates (or creates) the monthly balance entry if the product type requires tracking it
    log.info("Syncing Monthly Balance for new movement {}", movementDTO);
    MonthlyBalanceDTO monthlyBalanceDTO = null;
    if (syncedAccountDTO.productTypeShouldUpdateMonthlyBalance()) {
      if (movementToReverse) {
        monthlyBalanceDTO = monthlyBalanceService.syncForReversedMovement(movementDTO);
      } else {
        monthlyBalanceDTO = monthlyBalanceService.syncForNewMovement(movementDTO);
      }
    }

    return new AddMovementResultDTO(syncedAccountDTO, movementDTO, monthlyBalanceDTO);
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
    movementLifecycleService.save(movementDTO);
  }
}

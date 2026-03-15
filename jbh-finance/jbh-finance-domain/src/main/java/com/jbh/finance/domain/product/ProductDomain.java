package com.jbh.finance.domain.product;

import static com.jbh.commons.util.JbhMoneyUtils.JBH_ZERO;
import static com.jbh.commons.util.JbhMoneyUtils.isNegative;
import static com.jbh.commons.util.JbhMoneyUtils.isNegativeOrZero;
import static com.jbh.commons.util.JbhMoneyUtils.isZero;
import static com.jbh.commons.util.JbhMoneyUtils.withJBHDecimals;

import com.jbh.commons.exception.BusinessException;
import com.jbh.commons.exception.GenericSpecificationException;
import com.jbh.finance.domain.movement.MovementDomain;
import com.jbh.finance.domain.movement.vo.ProcessMovementOptionsVO;
import com.jbh.finance.domain.product.service.calculators.MoneyWeightedReturnCalculator;
import com.jbh.finance.domain.product.service.metrics.ProductMetricsCalculator;
import com.jbh.finance.domain.product.service.metrics.ProductMetricsCalculatorFactory;
import com.jbh.finance.domain.product.validation.ProductCreationValidator;
import com.jbh.finance.domain.product.validation.ProductCreationValidatorFactory;
import com.jbh.finance.domain.product.vo.ProductId;
import com.jbh.finance.domain.product.vo.ProductMetadata;
import com.jbh.finance.domain.product.vo.ProductMetadataKey;
import com.jbh.finance.domain.product.vo.ProductType;
import com.jbh.finance.domain.shared.exceptions.BusinessDomainExceptionType;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import lombok.Getter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Getter
@SuppressWarnings({
  "PMD.GodClass",
  "PMD.ExcessiveParameterList",
  "PMD.CollapsibleIfStatements",
  "PMD.TooFewBranchesForASwitchStatement"
})
public class ProductDomain {

  private static final Logger LOG = LoggerFactory.getLogger(ProductDomain.class);
  protected ProductId id;
  protected String name;
  protected ProductType type;
  protected UUID userId;
  protected BigDecimal movementBalance = JBH_ZERO;
  protected BigDecimal currentBalance = JBH_ZERO;

  /**
   * The profit balance is the difference between the current balance and the movement balance. It
   * can be negative because the user has not reported some movements
   */
  protected BigDecimal netProfitBalance = JBH_ZERO;

  protected boolean isActive = true;
  protected LocalDateTime createdAt = LocalDateTime.now();
  protected LocalDateTime updatedAt;

  /** Net growth rate is the rate of change of the net profit balance for the product. */
  protected BigDecimal netGrowthRate = JBH_ZERO;

  protected ProductMetadata metadata = ProductMetadata.empty();
  private ProductMetricsCalculator metricsCalculator;

  private ProductDomain(final String name, final ProductType type, final UUID userId) {
    this.id = ProductId.generate();
    this.name = name;
    this.type = type;
    this.userId = userId;
  }

  public ProductDomain(
      final ProductId id,
      final String name,
      final ProductType type,
      final UUID userId,
      final BigDecimal movementBalance,
      final BigDecimal currentBalance,
      final BigDecimal netProfitBalance,
      final boolean isActive,
      final LocalDateTime createdAt,
      final LocalDateTime updatedAt,
      final BigDecimal netGrowthRate,
      final ProductMetadata metadata) {
    this.id = id;
    this.name = name;
    this.type = type;
    this.userId = userId;
    this.movementBalance = movementBalance;
    this.currentBalance = currentBalance;
    this.netProfitBalance = netProfitBalance;
    this.isActive = isActive;
    this.createdAt = createdAt;
    this.updatedAt = updatedAt;
    this.netGrowthRate = netGrowthRate;
    this.metadata = metadata;
  }

  /**
   * Factory method to create an product with minimum required data for creation. Validates the
   * product based on type-specific requirements using the Strategy Pattern.
   *
   * @param name Account name
   * @param type Account type
   * @param userId User ID
   * @param inputMetadata Account inputMetadata (ProductMetadata instance)
   * @return AccountDomain instance
   * @throws BusinessException if validation fails based on product type requirements
   */
  public static ProductDomain withMinimumDataForCreation(
      final String name,
      final ProductType type,
      final UUID userId,
      final ProductMetadata inputMetadata)
      throws BusinessException {

    // Use provided inputMetadata or create empty if null
    final ProductMetadata productMetadata =
        inputMetadata != null ? inputMetadata : ProductMetadata.empty();

    // Validate inputMetadata based on product type BEFORE creating the domain object
    validateMetadata(type, productMetadata);

    // Only create the object if validation passes
    final ProductDomain accountDomain = new ProductDomain(name, type, userId);

    // Set inputMetadata if provided
    if (!productMetadata.isEmpty()) {
      accountDomain.metadata = productMetadata;
    }

    // Setting Current/Movement Balance with Common initial balance inputMetadata
    if (productMetadata.hasKey(ProductMetadataKey.COMMON_INITIAL_BALANCE)) {
      accountDomain.currentBalance = productMetadata.findCommonMetadata().getInitialBalance();
      accountDomain.movementBalance = accountDomain.currentBalance;
    }

    return accountDomain;
  }

  private static void validateMetadata(
      final ProductType inputType, final ProductMetadata inputMetadata) throws BusinessException {
    getValidator(inputType).validateMetadata(inputMetadata);
  }

  private static ProductCreationValidator getValidator(final ProductType type) {
    return ProductCreationValidatorFactory.getValidator(type);
  }

  public void syncBalancesByMovement(
      final MovementDomain movement, final ProcessMovementOptionsVO movementOptions)
      throws BusinessException {
    movement.validate();

    if (!this.getId().equals(movement.getProductId())) {
      throw new BusinessException(BusinessDomainExceptionType.PRODUCT_MISMATCH);
    }

    validateInsufficientNetFlow(movement);

    applyMovement(movement, movementOptions);
  }

  public void validateInsufficientNetFlow(final MovementDomain movement) throws BusinessException {
    getValidator(this.type).validateInsufficientNetFlow(this, movement);
  }

  /**
   * If it's a movement for an official monthly reported, the balance is already synced and it must
   * not change. The balance is updated because the movement was added after the monthly report was
   * created.
   *
   * <p>Otherwise, sync the balance and update the current balance.
   *
   * @param newAccountMovement
   * @param movementOptions
   */
  @SuppressWarnings("PMD.PrematureDeclaration")
  private void applyMovement(
      final MovementDomain newAccountMovement, final ProcessMovementOptionsVO movementOptions)
      throws BusinessException {
    final BigDecimal movementAmount = newAccountMovement.getMovementAmount();

    if (newAccountMovement.getMovementType() == null) {
      throw new GenericSpecificationException("Movement type cannot be null");
    }

    final boolean wasOfficialReport = movementOptions.isMonthOfficiallyReported();
    metricsCalculator = ProductMetricsCalculatorFactory.getCalculator(this.type);

    // If it's an official report, the monthly profit, and closing balance are already synced.
    // Movement balance will be synced due to a new movement done.
    if (wasOfficialReport) {
      syncMovementBalance(movementAmount, movementOptions);
      return;
    }

    final BigDecimal openingBalancePrevMovBalance = this.currentBalance;

    final boolean isInitialBalance = isInitialBalance();
    if (isInitialBalance) {
      putInitialBalanceMetadata(currentBalance);
    }

    syncMovementBalance(movementAmount, movementOptions);
    syncCurrentBalance(newAccountMovement, movementOptions);

    if (hasValidBalance()) {
      if (checkIfFullyWithdrawn(currentBalance, movementAmount)) {
        metadata.findCommonMetadata().putFullyWithdrawn(newAccountMovement.getMovementDate());
      }
    }

    var inputAmountForNetGrowthRate = movementAmount;
    var inputOpeningBalance = openingBalancePrevMovBalance;
    if (movementOptions.operation().toRemove()) {
      inputOpeningBalance = openingBalancePrevMovBalance.subtract(movementAmount);
      inputAmountForNetGrowthRate = BigDecimal.ZERO;
    }

    // Once the product is synced is ready to sync the profit balance and update metadata
    syncNetGrowthRate(inputOpeningBalance, inputAmountForNetGrowthRate);
    syncProfitBalance();
    updateMetadataFields(newAccountMovement);

    this.updatedAt = LocalDateTime.now();
    this.isActive = !newAccountMovement.isToCloseProduct();
  }

  private void syncMovementBalance(
      final BigDecimal movementAmount, final ProcessMovementOptionsVO movementOptions) {
    if (movementAmount == null) {
      return;
    }
    switch (movementOptions.operation()) {
      case ADD -> {
        this.movementBalance = this.movementBalance.add(movementAmount);
      }
      case REMOVE -> {
        this.movementBalance = this.movementBalance.subtract(movementAmount);
      }
    }
  }

  private boolean isInitialBalance() {
    return isZero(movementBalance) && isZero(currentBalance);
  }

  private void putInitialBalanceMetadata(final BigDecimal initialBalance) {
    metadata.findCommonMetadata().putInitialBalance(initialBalance);
  }

  private void syncCurrentBalance(
      final MovementDomain movement, final ProcessMovementOptionsVO movementOptions) {
    if (movement == null) {
      return;
    }

    if (movement.getMovementAmount() != null && movement.getMovementType().isNotBalanceSnapshot()) {
      switch (movementOptions.operation()) {
        case ADD -> {
          this.currentBalance = this.currentBalance.add(movement.getMovementAmount());
        }
        case REMOVE -> {
          this.currentBalance = this.currentBalance.subtract(movement.getMovementAmount());
        }
      }
    }

    final BigDecimal balanceSnapshot = movement.getBalanceSnapshot();
    if (balanceSnapshot != null) {
      this.currentBalance = balanceSnapshot;
    }
  }

  private boolean hasValidBalance() {
    return this.currentBalance != null
        && this.movementBalance != null
        && this.netProfitBalance != null;
  }

  public boolean checkIfFullyWithdrawn(
      final BigDecimal closingBalance, final BigDecimal movementAmount) {
    return isNegativeOrZero(closingBalance) && isNegative(movementAmount);
  }

  private void syncNetGrowthRate(final BigDecimal openingBalance, final BigDecimal movementAmount)
      throws BusinessException {
    if (hasValidBalance()) {
      try {
        this.netGrowthRate =
            metricsCalculator.calculateNetGrowthReate(openingBalance, this, movementAmount);
      } catch (final BusinessException e) {
        // FIXME figure out why it's failing
        // NU Test Closing Balance
        LOG.error("Error in calculating net Growth Rate {}", movementAmount);
      }
    }
  }

  private void syncProfitBalance() throws BusinessException {
    this.netProfitBalance = metricsCalculator.calculateProfitBalance(this);
  }

  private void updateMetadataFields(final MovementDomain movement) {
    this.metadata = metricsCalculator.updateMetadata(this, movement);
  }

  public void setCalculatedMoneyGrowthRate(
      final List<BigDecimal> cashFlows, final List<LocalDate> dates) {

    this.netGrowthRate = MoneyWeightedReturnCalculator.calculateXIRR(cashFlows, dates);
  }

  public boolean isFullyWithdrawn() {
    return metadata.findCommonMetadata().isFullyWithdrawn();
  }

  public boolean hasMetadata(final ProductMetadataKey key) {
    return metadata.hasKey(key);
  }

  public void setCurrentBalance(final BigDecimal closingBalance) {
    this.currentBalance = withJBHDecimals(closingBalance);
  }

  public void setCalculatedNetProfit(final BigDecimal inputNetProfit) {
    if (inputNetProfit != null) {
      this.netProfitBalance = withJBHDecimals(inputNetProfit);
    }
  }

  public void replaceAllMetadata(final ProductMetadata productMetadata) throws BusinessException {
    validateMetadata(this.type, productMetadata);
    this.metadata = ProductMetadata.fromMap(productMetadata.getData());
  }

  public void setName(final String name) {
    if (name == null || name.isBlank()) {
      throw new IllegalArgumentException("Product name cannot be null or blank");
    }
    this.name = name;
    this.updatedAt = LocalDateTime.now();
  }

  public void deactivate() {
    this.isActive = false;
    this.updatedAt = LocalDateTime.now();
  }

  public void activate() {
    this.isActive = true;
    this.updatedAt = LocalDateTime.now();
  }

  public void setActive(final boolean active) {
    this.isActive = active;
    this.updatedAt = LocalDateTime.now();
  }
}

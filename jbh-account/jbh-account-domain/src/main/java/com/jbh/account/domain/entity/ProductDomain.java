package com.jbh.account.domain.entity;

import static com.jbh.account.domain.utils.JbhMoneyUtils.JBH_ZERO;
import static com.jbh.account.domain.utils.JbhMoneyUtils.isNegative;
import static com.jbh.account.domain.utils.JbhMoneyUtils.isNegativeOrZero;
import static com.jbh.account.domain.utils.JbhMoneyUtils.isZero;
import static com.jbh.account.domain.utils.JbhMoneyUtils.withJBHDecimals;

import com.jbh.account.domain.calculators.MoneyWeightedReturnCalculator;
import com.jbh.account.domain.exceptions.AccountBusinessException;
import com.jbh.account.domain.exceptions.BusinessDomainExceptionType;
import com.jbh.account.domain.validation.account.creation.AccountCreationValidator;
import com.jbh.account.domain.validation.account.creation.AccountCreationValidatorFactory;
import com.jbh.account.domain.validation.account.metrics.AccountMetricsCalculator;
import com.jbh.account.domain.validation.account.metrics.AccountMetricsCalculatorFactory;
import com.jbh.account.domain.vo.AccountId;
import com.jbh.account.domain.vo.ProductMetadata;
import com.jbh.account.domain.vo.ProductMetadataKey;
import com.jbh.account.domain.vo.ProductType;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import lombok.Getter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Getter
@SuppressWarnings({"PMD.ExcessiveParameterList", "PMD.CollapsibleIfStatements"})
public class ProductDomain {

  private static final Logger LOG = LoggerFactory.getLogger(ProductDomain.class);
  protected AccountId id;
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

  /** Net growth rate is the rate of change of the net profit balance for the account. */
  protected BigDecimal netGrowthRate = JBH_ZERO;

  protected ProductMetadata metadata = ProductMetadata.empty();
  private AccountMetricsCalculator metricsCalculator;

  private ProductDomain(final String name, final ProductType type, final UUID userId) {
    this.id = AccountId.generate();
    this.name = name;
    this.type = type;
    this.userId = userId;
  }

  public ProductDomain(
      final AccountId id,
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
   * Factory method to create an account with minimum required data for creation. Validates the
   * account based on type-specific requirements using the Strategy Pattern.
   *
   * @param name Account name
   * @param type Account type
   * @param userId User ID
   * @param metadata Account metadata (ProductMetadata instance)
   * @return AccountDomain instance
   * @throws AccountBusinessException if validation fails based on account type requirements
   */
  public static ProductDomain withMinimumDataForCreation(
      final String name, final ProductType type, final UUID userId, final ProductMetadata metadata)
      throws AccountBusinessException {

    // Use provided metadata or create empty if null
    final ProductMetadata productMetadata = metadata != null ? metadata : ProductMetadata.empty();

    // Validate metadata based on account type BEFORE creating the domain object
    final AccountCreationValidator validator = getValidator(type);
    validator.validateMetadata(productMetadata);

    // Only create the object if validation passes
    final ProductDomain accountDomain = new ProductDomain(name, type, userId);

    // Set metadata if provided
    if (!productMetadata.isEmpty()) {
      accountDomain.metadata = productMetadata;
    }

    return accountDomain;
  }

  private static AccountCreationValidator getValidator(final ProductType type) {
    return AccountCreationValidatorFactory.getValidator(type);
  }

  public void syncBalancesByMovement(
      final AccountMovementDomain movement, final boolean wasOfficialReport)
      throws AccountBusinessException {
    movement.validate();

    if (!this.getId().equals(movement.getAccountId())) {
      throw new AccountBusinessException(BusinessDomainExceptionType.ACCOUNT_MISMATCH);
    }

    validateInsufficientNetFlow(movement);

    applyMovement(movement, wasOfficialReport);
  }

  private void validateInsufficientNetFlow(final AccountMovementDomain movement)
      throws AccountBusinessException {
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
   * @param wasOfficialReport
   */
  private void applyMovement(
      final AccountMovementDomain newAccountMovement, final boolean wasOfficialReport)
      throws AccountBusinessException {
    final BigDecimal movementAmount = newAccountMovement.getMovementAmount();

    metricsCalculator = AccountMetricsCalculatorFactory.getCalculator(this.type);

    // If it's an official report, the monthly profit, and closing balance are already synced.
    // Movement balance will be synced due to a new movement done.
    if (wasOfficialReport) {
      addAmountToMovementBalance(movementAmount);
      return;
    }

    final BigDecimal openingBalance = this.movementBalance;

    final boolean isInitialBalance = isInitialBalance();

    if (movementAmount != null) {
      addAmountToMovementBalance(movementAmount);
      this.currentBalance = this.currentBalance.add(movementAmount);
    }

    final BigDecimal balanceSnapshot = newAccountMovement.getBalanceSnapshot();
    if (balanceSnapshot != null) {
      this.currentBalance = balanceSnapshot;
    }
    if (isInitialBalance) {
      putInitialBalanceMetadata(currentBalance);
    }

    if (hasValidBalance()) {
      if (checkIfFullyWithdrawn(currentBalance, movementAmount)) {
        metadata.putFullyWithdrawn(newAccountMovement.getMovementDate());
      }
    }

    syncNetGrowthRate(openingBalance, movementAmount);
    this.isActive = !newAccountMovement.isToCloseProduct();

    syncProfitBalance();
    this.updatedAt = LocalDateTime.now();
  }

  private void addAmountToMovementBalance(final BigDecimal movementAmount) {
    this.movementBalance = this.movementBalance.add(movementAmount);
  }

  private boolean isInitialBalance() {
    return isZero(movementBalance) && isZero(currentBalance);
  }

  private void putInitialBalanceMetadata(final BigDecimal initialBalance) {
    metadata.putInitialBalance(initialBalance);
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
      throws AccountBusinessException {
    if (hasValidBalance()) {
      try {
        this.netGrowthRate =
            metricsCalculator.calculateNetGrowthReate(openingBalance, this, movementAmount);
      } catch (final AccountBusinessException e) {
        // FIXME figure out why it's failing
        // NU Test Closing Balance
        LOG.error("Error in calculating net Growth Rate {}", movementAmount);
      }
    }
  }

  private void syncProfitBalance() throws AccountBusinessException {
    this.netProfitBalance = metricsCalculator.calculateProfitBalance(this);
  }

  public void setCalculatedMoneyGrowthRate(
      final List<BigDecimal> cashFlows, final List<LocalDate> dates) {

    this.netGrowthRate = MoneyWeightedReturnCalculator.calculateXIRR(cashFlows, dates);
  }

  public boolean isFullyWithdrawn() {
    return metadata.isFullyWithdrawn();
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
}

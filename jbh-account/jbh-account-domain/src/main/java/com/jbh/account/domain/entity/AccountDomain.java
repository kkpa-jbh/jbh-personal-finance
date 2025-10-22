package com.jbh.account.domain.entity;

import static com.jbh.account.domain.utils.JbhMoneyUtils.JBH_ZERO;
import static com.jbh.account.domain.utils.JbhMoneyUtils.withJBHDecimals;

import com.jbh.account.domain.calculators.MoneyGrowthCalculator;
import com.jbh.account.domain.calculators.MoneyWeightedReturnCalculator;
import com.jbh.account.domain.exceptions.AccountBusinessException;
import com.jbh.account.domain.exceptions.BusinessDomainExceptionType;
import com.jbh.account.domain.utils.JbhBooleanUtils;
import com.jbh.account.domain.utils.JbhMoneyUtils;
import com.jbh.account.domain.vo.AccountId;
import com.jbh.account.domain.vo.AccountMetadataKey;
import com.jbh.account.domain.vo.AccountType;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import lombok.Getter;

@Getter
@SuppressWarnings("PMD.ExcessiveParameterList")
public class AccountDomain {

  private final MoneyGrowthCalculator moneyGrowthCalculator = new MoneyGrowthCalculator();
  protected AccountId id;
  protected String name;
  protected AccountType type;
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

  protected Map<String, Object> metadata = new HashMap<>();

  public AccountDomain() {
    this.id = AccountId.generate();
  }

  private AccountDomain(final AccountId id, final UUID userId) {
    this.userId = userId;
    this.id = id;
  }

  public AccountDomain(
      final AccountId id,
      final String name,
      final AccountType type,
      final UUID userId,
      final BigDecimal movementBalance,
      final BigDecimal currentBalance,
      final BigDecimal netProfitBalance,
      final boolean isActive,
      final LocalDateTime createdAt,
      final LocalDateTime updatedAt,
      final BigDecimal netGrowthRate,
      final Map<String, Object> metadata) {
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

  public static AccountDomain withMinimumDataForCreation(
      final String name, final AccountType type, final UUID userId) {
    final AccountDomain accountDomain = new AccountDomain();
    accountDomain.name = name;
    accountDomain.type = type;
    accountDomain.userId = userId;
    return accountDomain;
  }

  public static AccountDomain withBasicMovementForExisting(
      final AccountId accountId,
      final UUID userId,
      final BigDecimal movementBalance,
      final BigDecimal currentBalance) {
    final AccountDomain accountDomain = new AccountDomain(accountId, userId);
    accountDomain.movementBalance = movementBalance;
    accountDomain.currentBalance = currentBalance;
    return accountDomain;
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
    final BigDecimal mvmtAmount = movement.getMovementAmount();
    final boolean isNegativeAmount = mvmtAmount != null && mvmtAmount.signum() < 0;
    if (isNegativeAmount) {
      final BigDecimal possibleCurrentBalance = this.currentBalance.add(mvmtAmount);
      final boolean isNegativeCurrentBalance = possibleCurrentBalance.signum() < 0;
      if (isNegativeCurrentBalance) {
        throw new AccountBusinessException(BusinessDomainExceptionType.INSUFFICIENT_FUNDS);
      }
    }
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

    // If it's an official report, the monthly profit, and closing balance are already synced.
    // Movement balance will be synced due to a new movement done.
    if (wasOfficialReport) {
      addAmountToMovementBalance(movementAmount);
      return;
    }

    final BigDecimal openingBalance = this.movementBalance;

    if (movementAmount != null) {
      addAmountToMovementBalance(movementAmount);
      this.currentBalance = this.currentBalance.add(movementAmount);
    }

    final BigDecimal balanceSnapshot = newAccountMovement.getBalanceSnapshot();
    if (balanceSnapshot != null) {
      this.currentBalance = balanceSnapshot;
    }

    syncProfitBalance();
    syncNetGrowthRate(openingBalance, newAccountMovement);
    this.updatedAt = LocalDateTime.now();
  }

  private void addAmountToMovementBalance(final BigDecimal movementAmount) {
    this.movementBalance = this.movementBalance.add(movementAmount);
  }

  private void syncProfitBalance() {
    this.netProfitBalance = this.currentBalance.subtract(this.movementBalance);
  }

  private void syncNetGrowthRate(
      final BigDecimal openingBalance, final AccountMovementDomain newAccountMovement)
      throws AccountBusinessException {
    if (this.currentBalance != null
        && this.movementBalance != null
        && this.netProfitBalance != null) {

      // Investments
      final BigDecimal closingBalance =
          newAccountMovement.getBalanceSnapshot() != null
              ? newAccountMovement.getBalanceSnapshot()
              : this.currentBalance;
      final BigDecimal movementAmount =
          newAccountMovement.getMovementAmount() != null
              ? newAccountMovement.getMovementAmount()
              : JBH_ZERO;

      if (checkIfFullyWithdrawn(closingBalance, movementAmount)) {
        addMetadata(AccountMetadataKey.FULLY_WITHDRAWN, true);
        addMetadata(AccountMetadataKey.FULLY_WITHDRAWN_DATE, newAccountMovement.getMovementDate());
        addMetadata(AccountMetadataKey.FULLY_WITHDRAWN_AT, LocalDateTime.now());
        return;
      }

      // Regular net growth rate calculation
      this.netGrowthRate =
          moneyGrowthCalculator.calculateGrowth(openingBalance, closingBalance, movementAmount);
    }
  }

  public boolean checkIfFullyWithdrawn(
      final BigDecimal closingBalance, final BigDecimal movementAmount) {
    return JbhMoneyUtils.isZero(closingBalance) && movementAmount.signum() < 0;
  }

  private void addMetadata(final AccountMetadataKey key, final Object value) {
    metadata.put(key.name(), value);
  }

  public void setCalculatedMoneyGrowthRate(
      final List<BigDecimal> cashFlows, final List<LocalDate> dates) {

    this.netGrowthRate = MoneyWeightedReturnCalculator.calculateXIRR(cashFlows, dates);
  }

  public boolean isFullyWithdrawn() {
    return hasMetadata(AccountMetadataKey.FULLY_WITHDRAWN)
        && JbhBooleanUtils.isTrue(getMetadataField(AccountMetadataKey.FULLY_WITHDRAWN));
  }

  public boolean hasMetadata(final AccountMetadataKey key) {
    return metadata != null && metadata.containsKey(key.name());
  }

  public Object getMetadataField(final AccountMetadataKey key) {
    return metadata != null ? metadata.get(key.name()) : null;
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

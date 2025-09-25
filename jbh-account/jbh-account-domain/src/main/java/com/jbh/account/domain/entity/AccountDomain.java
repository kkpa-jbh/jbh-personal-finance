package com.jbh.account.domain.entity;

import com.jbh.account.domain.exceptions.GenericSpecificationException;
import com.jbh.account.domain.utils.MoneyUtils;
import com.jbh.account.domain.vo.AccountId;
import com.jbh.account.domain.vo.AccountType;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class AccountDomain {

  protected AccountId id;
  protected String name;
  protected AccountType type;
  protected UUID userId;
  protected BigDecimal movementBalance = MoneyUtils.JBH_ZERO;
  protected BigDecimal currentBalance = MoneyUtils.JBH_ZERO;
  protected BigDecimal profitBalance = MoneyUtils.JBH_ZERO;
  protected boolean isActive = true;
  protected LocalDateTime createdAt = LocalDateTime.now();
  protected LocalDateTime updatedAt;

  /**
   * Nominal/stated percentage (what the bank advertises) "This account offers 3% annual interest
   * rate"
   */
  private BigDecimal advertisedAnnualRate;

  /**
   * Yield = Actual return earned (includes compounding effects) Compound Interest = Interest
   * compuesto. This account yielded 3.15% annually after monthly compounding
   */
  private BigDecimal estimatedAnnualYield;

  public AccountDomain() {
    this.id = AccountId.generate();
  }

  private AccountDomain(final AccountId id, final UUID userId) {
    this.userId = userId;
    this.id = id;
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

  public void syncBalancesWithUploadedMovements(
      final List<AccountMovementDomain> multipleMovements) {
    if (multipleMovements == null || multipleMovements.isEmpty()) {
      throw new GenericSpecificationException("Movements cannot be null or empty");
    }
    final List<AccountMovementDomain> filteredMovements =
        multipleMovements.stream().filter(Objects::nonNull).toList();

    for (final AccountMovementDomain movement : filteredMovements) {
      syncBalancesByMovement(movement, false);
    }
  }

  public void syncBalancesByMovement(
      final AccountMovementDomain movement, final boolean wasOfficialReport) {
    movement.validate();

    if (!this.getId().equals(movement.getAccountId())) {
      throw new GenericSpecificationException("Account ID mismatch when applying movement");
    }

    validateInsufficientNetFlow(movement);

    applyMovement(movement, wasOfficialReport);
  }

  private void validateInsufficientNetFlow(final AccountMovementDomain movement) {
    final BigDecimal mvmtAmount = movement.getMovementAmount();
    final boolean isNegativeAmount = mvmtAmount != null && mvmtAmount.signum() < 0;
    if (isNegativeAmount) {
      final BigDecimal possibleCurrentBalance = this.currentBalance.add(mvmtAmount);
      final boolean isNegativeCurrentBalance = possibleCurrentBalance.signum() < 0;
      if (isNegativeCurrentBalance) {
        throw new GenericSpecificationException("Insufficient effective balance");
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
      final AccountMovementDomain newAccountMovement, final boolean wasOfficialReport) {
    final BigDecimal movementAmount = newAccountMovement.getMovementAmount();

    // If it's an official report, the balance is already synced.
    //
    if (wasOfficialReport) {
      syncMovementBalance(movementAmount);
      syncProfitBalance();
      return;
    }

    if (movementAmount != null) {
      syncMovementBalance(movementAmount);
      this.currentBalance = this.currentBalance.add(movementAmount);
    }

    final BigDecimal balanceSnapshot = newAccountMovement.getBalanceSnapshot();
    if (balanceSnapshot != null) {
      this.currentBalance = balanceSnapshot;
    }

    syncProfitBalance();
    this.updatedAt = LocalDateTime.now();
  }

  private void syncMovementBalance(final BigDecimal movementAmount) {
    this.movementBalance = this.movementBalance.add(movementAmount);
  }

  private void syncProfitBalance() {
    this.profitBalance = this.currentBalance.subtract(this.movementBalance);
  }

  public void syncByMonthlyReport(final BigDecimal closingBalance) {
    this.currentBalance = closingBalance;
  }
}

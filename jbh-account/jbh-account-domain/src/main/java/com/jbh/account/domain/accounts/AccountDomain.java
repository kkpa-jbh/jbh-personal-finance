package com.jbh.account.domain.accounts;

import com.jbh.account.domain.exceptions.GenericSpecificationException;
import com.jbh.account.domain.movements.AccountMovementDomain;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import lombok.Data;

@Data
public final class AccountDomain {

  private final AccountId id;
  private String name;
  private Long userId;
  private BigDecimal movementBalance;
  private BigDecimal currentBalance;
  private BigDecimal profitBalance;

  private LocalDateTime createdAt;
  private LocalDateTime updatedAt;

  private AccountDomain(final AccountId id) {
    this.id = id;
    this.movementBalance = BigDecimal.ZERO;
    this.currentBalance = BigDecimal.ZERO;
    this.createdAt = LocalDateTime.now();
    this.updatedAt = LocalDateTime.now();
  }

  public static AccountDomain withId(final AccountId id) {
    return new AccountDomain(id);
  }

  public void syncBalances(final List<AccountMovementDomain> multipleMovements) {
    if (multipleMovements == null || multipleMovements.isEmpty()) {
      throw new GenericSpecificationException("Movements cannot be null or empty");
    }
    final List<AccountMovementDomain> filteredMovements = multipleMovements.stream().filter(Objects::nonNull)
        .toList();

    for (final AccountMovementDomain movement : filteredMovements) {
      syncBalances(movement);
    }
  }

  public void syncBalances(final AccountMovementDomain movement) {
    movement.validate();

    if (!this.getId().equals(movement.getAccountId())) {
      throw new GenericSpecificationException("Account ID mismatch when applying movement");
    }

    final BigDecimal mvmtAmount = movement.getMovementAmount();
    final boolean isNegativeAmount = mvmtAmount != null && mvmtAmount.signum() < 0;
    if (isNegativeAmount) {
      final BigDecimal possibleCurrentBalance = this.currentBalance.add(mvmtAmount);
      final boolean isNegativeCurrentBalance = possibleCurrentBalance.signum() < 0;
      if (isNegativeCurrentBalance) {
        throw new GenericSpecificationException("Insufficient effective balance");
      }
    }

    applyMovement(movement);
  }


  private void applyMovement(final AccountMovementDomain newAccountMovement) {
    final BigDecimal movementAmount = newAccountMovement.getMovementAmount();
    if (movementAmount != null) {
      this.movementBalance = this.movementBalance.add(movementAmount);
      this.currentBalance = this.currentBalance.add(movementAmount);
    }

    final BigDecimal balanceSnapshot = newAccountMovement.getBalanceSnapshot();
    if (balanceSnapshot != null) {
      this.currentBalance = balanceSnapshot;
    }

    this.profitBalance = this.currentBalance.subtract(this.movementBalance);
    this.updatedAt = LocalDateTime.now();

  }
}

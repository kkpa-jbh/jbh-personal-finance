package com.jbh.account.domain.accounts;

import com.jbh.account.domain.exceptions.GenericSpecificationException;
import com.jbh.account.domain.movements.AccountMovementDomain;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import lombok.Data;

@Data
public final class AccountDomain {

  private AccountId id;
  private String name;
  private Long userId;
  private BigDecimal movementBalance;
  private BigDecimal currentBalance;
  private BigDecimal profitBalance;

  private LocalDateTime createdAt;
  private LocalDateTime updatedAt;

  private AccountDomain() {
    this.movementBalance = BigDecimal.ZERO;
    this.currentBalance = BigDecimal.ZERO;
    this.createdAt = LocalDateTime.now();
    this.updatedAt = LocalDateTime.now();
  }

  public static AccountDomain withId(AccountId id) {
    AccountDomain account = new AccountDomain();
    account.setId(id);
    return account;
  }

  public void syncBalances(List<AccountMovementDomain> multipleMovements) {
    if (multipleMovements == null || multipleMovements.isEmpty()) {
      throw new GenericSpecificationException("Movements cannot be null or empty");
    }
    List<AccountMovementDomain> filteredMovements = multipleMovements.stream().filter(Objects::nonNull)
        .collect(Collectors.toList());

    for (AccountMovementDomain movement : filteredMovements) {
      syncBalances(movement);
    }
  }

  public void syncBalances(AccountMovementDomain movement) {
    if (movement == null) {
      throw new GenericSpecificationException("Movement cannot be null");
    }
    if (movement.getMovementDate() == null) {
      throw new GenericSpecificationException("Movement date cannot be null");
    }
    if (movement.getMovementAmount() == null && movement.getBalanceSnapshot() == null) {
      throw new GenericSpecificationException("Movement amount cannot be null");
    }

    if (movement.getMovementAmount() != null && movement.getMovementAmount().signum() < 0
        && this.currentBalance.add(movement.getMovementAmount()).signum() < 0) {
      throw new GenericSpecificationException("Insufficient effective balance");
    }

    applyMovement(movement);
  }


  private void applyMovement(AccountMovementDomain newAccountMovement) {
    if (newAccountMovement.getMovementAmount() != null) {
      this.movementBalance = this.movementBalance.add(newAccountMovement.getMovementAmount());
      this.currentBalance = this.currentBalance.add(newAccountMovement.getMovementAmount());
    }

    if (newAccountMovement.getBalanceSnapshot() != null) {
      this.currentBalance = newAccountMovement.getBalanceSnapshot();
    }

    this.profitBalance = this.currentBalance.subtract(this.movementBalance);
    this.updatedAt = LocalDateTime.now();

  }
}

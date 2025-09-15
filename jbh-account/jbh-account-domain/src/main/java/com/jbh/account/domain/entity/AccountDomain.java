package com.jbh.account.domain.entity;

import com.jbh.account.domain.exceptions.GenericSpecificationException;
import com.jbh.account.domain.utils.MoneyUtils;
import com.jbh.account.domain.vo.AccountDomainDTO;
import com.jbh.account.domain.vo.AccountId;
import com.jbh.account.domain.vo.AccountType;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.experimental.SuperBuilder;

@AllArgsConstructor
@Getter
@SuperBuilder
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

  private AccountDomain() {
    this.id = AccountId.generate();
  }

  private AccountDomain(final AccountId id) {
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
      final BigDecimal movementBalance,
      final BigDecimal currentBalance) {
    final AccountDomain accountDomain = new AccountDomain(accountId);
    accountDomain.movementBalance = movementBalance;
    accountDomain.currentBalance = currentBalance;
    return accountDomain;
  }

  public void syncBalances(final List<AccountMovementDomain> multipleMovements) {
    if (multipleMovements == null || multipleMovements.isEmpty()) {
      throw new GenericSpecificationException("Movements cannot be null or empty");
    }
    final List<AccountMovementDomain> filteredMovements =
        multipleMovements.stream().filter(Objects::nonNull).toList();

    for (final AccountMovementDomain movement : filteredMovements) {
      syncSingleBalance(movement);
    }
  }

  private void syncSingleBalance(final AccountMovementDomain movement) {
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

  public AccountDomainDTO toDTO() {
    return clone(this);
  }

  private AccountDomainDTO clone(final AccountDomain account) {
    return AccountDomainDTO.builder()
        .id(account.getId())
        .name(account.getName())
        .type(account.getType())
        .userId(account.getUserId())
        .movementBalance(account.getMovementBalance())
        .currentBalance(account.getCurrentBalance())
        .profitBalance(account.getProfitBalance())
        .createdAt(account.getCreatedAt())
        .updatedAt(account.getUpdatedAt())
        .build();
  }
}

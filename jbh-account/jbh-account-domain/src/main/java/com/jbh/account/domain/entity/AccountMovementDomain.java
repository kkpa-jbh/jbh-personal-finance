package com.jbh.account.domain.entity;

import com.jbh.account.domain.exceptions.GenericSpecificationException;
import com.jbh.account.domain.utils.MoneyUtils;
import com.jbh.account.domain.vo.AccountId;
import com.jbh.account.domain.vo.AccountMovementDTO;
import com.jbh.account.domain.vo.AccountMovementId;
import com.jbh.account.domain.vo.MovementType;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;
import lombok.Data;
import lombok.experimental.SuperBuilder;

@Data
@SuperBuilder
public class AccountMovementDomain {

  private final AccountMovementId id;
  private final AccountId accountId;
  private final MovementType movementType;
  private final BigDecimal movementAmount;
  private final LocalDate movementDate;
  private final BigDecimal balanceSnapshot;
  private final Map<String, Object> metadata;
  private final String description;

  public AccountMovementDomain(
      final AccountId accountId,
      final MovementType movementType,
      final LocalDate movementDate,
      final BigDecimal movementAmount,
      final BigDecimal balanceSnapshot,
      final Map<String, Object> metadata,
      final String description) {
    this.id = AccountMovementId.generate();
    this.accountId = accountId;
    this.movementDate = movementDate;
    this.movementAmount = movementAmount;
    this.movementType = movementType;
    this.balanceSnapshot = balanceSnapshot;
    this.metadata = metadata;
    this.description = description;
  }

  public static AccountMovementDomain of(
      final AccountId accountId, final LocalDate movementDate, final BigDecimal totalAmount) {
    return of(accountId, movementDate, totalAmount, MoneyUtils.JBH_ZERO);
  }

  public static AccountMovementDomain of(
      final AccountId accountId,
      final LocalDate movementDate,
      final BigDecimal totalAmount,
      final BigDecimal balanceSnapshot) {
    final MovementType movementType = findMovementTypeBaseOnAmounts(totalAmount, balanceSnapshot);
    final AccountMovementDomain movDomain =
        new AccountMovementDomain(
            accountId,
            movementType,
            movementDate,
            totalAmount,
            balanceSnapshot,
            new HashMap<>(),
            null);
    movDomain.validate();
    return movDomain;
  }

  private static MovementType findMovementTypeBaseOnAmounts(
      final BigDecimal totalAmount, final BigDecimal balanceSnapshot) {
    MovementType movementType = null;

    if (totalAmount != null) {
      movementType =
          totalAmount.compareTo(BigDecimal.ZERO) >= 0
              ? MovementType.DEPOSIT
              : MovementType.WITHDRAWAL;
    } else if (balanceSnapshot != null) {
      movementType = MovementType.BALANCE_SNAPSHOT;
    }
    return movementType;
  }

  public void validate() {
    validateAccountId();
    validateMovementType();
    validateMovementDate();
    validateMovementDateNotFuture();
    validateAmountOrSnapshot();
  }

  private void validateAccountId() {
    if (accountId == null || accountId.value() == null) {
      throw new GenericSpecificationException("Account ID cannot be null");
    }
  }

  private void validateMovementType() {
    if (movementType == null) {
      throw new GenericSpecificationException("Movement type cannot be null");
    }
  }

  private void validateMovementDate() {
    if (movementDate == null) {
      throw new GenericSpecificationException("Movement date cannot be null");
    }
  }

  private void validateMovementDateNotFuture() {
    if (movementDate != null && movementDate.isAfter(LocalDate.now())) {
      throw new GenericSpecificationException("Movement date cannot be in the future");
    }
  }

  private void validateAmountOrSnapshot() {
    if (movementAmount == null && balanceSnapshot == null) {
      throw new GenericSpecificationException("Total amount cannot be null");
    }
  }

  public static AccountMovementDomain withFileImport(
      final AccountId accountId,
      final LocalDate movementDate,
      final BigDecimal totalAmount,
      final BigDecimal balanceSnapshot) {
    final AccountMovementDomain movementDomain =
        of(accountId, movementDate, totalAmount, balanceSnapshot);
    movementDomain.addMetadata("fileImport", true);
    return movementDomain;
  }

  public void addMetadata(final String key, final Object value) {
    metadata.put(key, value);
  }

  public Object getMetadata(final String key) {
    return metadata != null ? metadata.get(key) : null;
  }

  public boolean hasMetadata(final String key) {
    return metadata != null && metadata.containsKey(key);
  }

  public AccountMovementDTO toDTO() {
    return AccountMovementDTO.builder()
        .id(id)
        .accountId(accountId)
        .movementType(movementType)
        .movementAmount(movementAmount)
        .movementDate(movementDate)
        .balanceSnapshot(balanceSnapshot)
        .metadata(metadata)
        .description(description)
        .build();
  }
}

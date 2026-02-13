package com.jbh.finance.domain.movement.vo;

import java.math.BigDecimal;

public enum MovementType {
  /** A deposit is a movement that has a positive total amount. It is used to update the product */
  DEPOSIT,
  /**
   * A withdrawal is a movement that has a negative total amount. It is used to update the product
   * balance.
   */
  WITHDRAWAL,
  /**
   * Balance snapshot is a movement that does not have a total amount. It is used to update the
   * product balance.
   */
  BALANCE_SNAPSHOT;

  /**
   * Total Amount value is the first citizen. Otherwise, Balance Snapshot is the second citizen.
   *
   * @param totalAmount
   * @param balanceSnapshot
   * @return
   */
  public static MovementType findByTotalAmountAndBalanceSnapshot(
      final BigDecimal totalAmount, final BigDecimal balanceSnapshot) {
    MovementType movementType = null;

    if (totalAmount != null) {
      movementType = totalAmount.compareTo(BigDecimal.ZERO) >= 0 ? DEPOSIT : WITHDRAWAL;
    } else if (balanceSnapshot != null) {
      movementType = BALANCE_SNAPSHOT;
    }
    return movementType;
  }

  public static MovementType findByCategory(final MovementCategoryVO movementCategoryDTO) {
    if (movementCategoryDTO == null) {
      throw new IllegalArgumentException("Category cannot be null");
    }
    return movementCategoryDTO.getSource() == CategorySource.INCOME ? DEPOSIT : WITHDRAWAL;
  }

  public boolean isDeposit() {
    return this == DEPOSIT;
  }

  public boolean isWithdrawal() {
    return this == WITHDRAWAL;
  }

  public boolean isBalanceSnapshot() {
    return this == BALANCE_SNAPSHOT;
  }
}

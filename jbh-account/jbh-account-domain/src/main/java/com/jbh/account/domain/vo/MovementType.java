package com.jbh.account.domain.vo;

import java.math.BigDecimal;

public enum MovementType {
  /** A deposit is a movement that has a positive total amount. It is used to update the account */
  DEPOSIT,
  /**
   * A withdrawal is a movement that has a negative total amount. It is used to update the account
   * balance.
   */
  WITHDRAWAL,
  /**
   * Balance snapshot is a movement that does not have a total amount. It is used to update the
   * account balance.
   */
  BALANCE_SNAPSHOT;

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

  public static MovementType findByCategory(final MovementCategoryDTO movementCategoryDTO) {
    if (movementCategoryDTO == null) {
      throw new IllegalArgumentException("Category cannot be null");
    }
    return movementCategoryDTO.getSource() == CategorySource.INCOME ? DEPOSIT : WITHDRAWAL;
  }
}

package com.jbh.finance.domain.movement.vo;

import com.jbh.finance.domain.category.vo.CategorySourceVO;
import com.jbh.finance.domain.category.vo.CategoryTypeVO;
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

  public static MovementType findByCategory(final CategoryTypeVO categoryType) {
    if (categoryType == null) {
      throw new IllegalArgumentException("Category cannot be null");
    }
    return categoryType.getSource() == CategorySourceVO.INCOME ? DEPOSIT : WITHDRAWAL;
  }

  public boolean isDeposit() {
    return this == DEPOSIT;
  }

  public boolean isWithdrawal() {
    return this == WITHDRAWAL;
  }

  public boolean isNotBalanceSnapshot() {
    return !isBalanceSnapshot();
  }

  public boolean isBalanceSnapshot() {
    return this == BALANCE_SNAPSHOT;
  }
}

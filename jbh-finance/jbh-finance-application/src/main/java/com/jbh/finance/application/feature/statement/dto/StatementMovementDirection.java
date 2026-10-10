package com.jbh.finance.application.feature.statement.dto;

import com.jbh.finance.domain.category.vo.CategorySourceVO;

/** Direction of a statement movement. The amount is always positive; this gives the sign. */
public enum StatementMovementDirection {
  CREDIT,
  DEBIT;

  /**
   * Maps the direction to the category source used to classify the movement.
   *
   * @return {@code INCOME} for {@code CREDIT}, {@code EXPENSE} for {@code DEBIT}
   */
  public CategorySourceVO toCategorySource() {
    return this == CREDIT ? CategorySourceVO.INCOME : CategorySourceVO.EXPENSE;
  }
}

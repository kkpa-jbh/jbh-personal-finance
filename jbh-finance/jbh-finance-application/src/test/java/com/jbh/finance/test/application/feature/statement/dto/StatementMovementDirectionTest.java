package com.jbh.finance.test.application.feature.statement.dto;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.jbh.finance.application.feature.statement.dto.StatementMovementDirection;
import com.jbh.finance.domain.category.vo.CategorySourceVO;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

@Tag("unit")
class StatementMovementDirectionTest {

  @Test
  void shouldMapToIncomeWhenCredit() {
    // Act
    final CategorySourceVO source = StatementMovementDirection.CREDIT.toCategorySource();

    // Assert
    assertEquals(CategorySourceVO.INCOME, source);
  }

  @Test
  void shouldMapToExpenseWhenDebit() {
    // Act
    final CategorySourceVO source = StatementMovementDirection.DEBIT.toCategorySource();

    // Assert
    assertEquals(CategorySourceVO.EXPENSE, source);
  }
}

package com.jbh.finance.domain.movement;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.jbh.commons.exception.BusinessException;
import com.jbh.finance.domain.movement.vo.MovementMetadata;
import com.jbh.finance.domain.movement.vo.MovementType;
import com.jbh.finance.domain.product.vo.ProductId;
import com.jbh.finance.domain.shared.exceptions.BusinessDomainExceptionType;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Map;
import org.junit.jupiter.api.Test;

class MovementDomainTest {

  private static final BigDecimal AMOUNT = new BigDecimal("100");

  @Test
  void shouldFailWithEmptyMovementDateWhenTheDateIsNull() {
    final MovementDomain movement = depositOn(null);

    final BusinessException exception = assertThrows(BusinessException.class, movement::validate);

    assertEquals(
        BusinessDomainExceptionType.EMPTY_MOVEMENT_DATE, exception.getBusinessExceptionType());
  }

  @Test
  void shouldFailWithEmptyMovementTypeWhenTheTypeIsNull() {
    final MovementDomain movement =
        new MovementDomain(
            ProductId.generate(),
            null,
            LocalDate.now(),
            AMOUNT,
            null,
            MovementMetadata.of(Map.of()),
            null,
            null);

    final BusinessException exception = assertThrows(BusinessException.class, movement::validate);

    assertEquals(
        BusinessDomainExceptionType.EMPTY_MOVEMENT_TYPE, exception.getBusinessExceptionType());
  }

  private static MovementDomain depositOn(final LocalDate movementDate) {
    return new MovementDomain(
        ProductId.generate(),
        MovementType.DEPOSIT,
        movementDate,
        AMOUNT,
        null,
        MovementMetadata.of(Map.of()),
        null,
        null);
  }
}

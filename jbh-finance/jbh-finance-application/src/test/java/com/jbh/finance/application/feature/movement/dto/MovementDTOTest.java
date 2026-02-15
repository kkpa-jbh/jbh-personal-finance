package com.jbh.finance.application.feature.movement.dto;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.jbh.finance.domain.movement.vo.MovementId;
import com.jbh.finance.domain.movement.vo.MovementType;
import com.jbh.finance.domain.product.vo.ProductId;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;

class MovementDTOTest {

  @Test
  void movement_created_in_current_month_can_be_removed() {
    final MovementDTO movement =
        MovementDTO.builder()
            .id(MovementId.generate())
            .productId(ProductId.generate())
            .movementType(MovementType.DEPOSIT)
            .movementAmount(new BigDecimal("100.00"))
            .movementDate(LocalDate.now())
            .balanceSnapshot(new BigDecimal("1000.00"))
            .createdAt(LocalDateTime.now())
            .description("Test movement")
            .build();

    assertTrue(movement.canBeRemoved());
  }

  @Test
  void movement_created_in_previous_month_cannot_be_removed() {
    final MovementDTO movement =
        MovementDTO.builder()
            .id(MovementId.generate())
            .productId(ProductId.generate())
            .movementType(MovementType.DEPOSIT)
            .movementAmount(new BigDecimal("100.00"))
            .movementDate(LocalDate.now())
            .balanceSnapshot(new BigDecimal("1000.00"))
            .createdAt(LocalDateTime.now().minusMonths(1))
            .description("Test movement")
            .build();

    assertFalse(movement.canBeRemoved());
  }

  @Test
  void movement_created_two_months_ago_cannot_be_removed() {
    final MovementDTO movement =
        MovementDTO.builder()
            .id(MovementId.generate())
            .productId(ProductId.generate())
            .movementType(MovementType.WITHDRAWAL)
            .movementAmount(new BigDecimal("50.00"))
            .movementDate(LocalDate.now())
            .balanceSnapshot(new BigDecimal("950.00"))
            .createdAt(LocalDateTime.now().minusMonths(2))
            .description("Old movement")
            .build();

    assertFalse(movement.canBeRemoved());
  }

  @Test
  void movement_with_null_createdAt_cannot_be_removed() {
    final MovementDTO movement =
        MovementDTO.builder()
            .id(MovementId.generate())
            .productId(ProductId.generate())
            .movementType(MovementType.DEPOSIT)
            .movementAmount(new BigDecimal("100.00"))
            .movementDate(LocalDate.now())
            .balanceSnapshot(new BigDecimal("1000.00"))
            .createdAt(null)
            .description("Movement without creation date")
            .build();

    assertFalse(movement.canBeRemoved());
  }

  @Test
  void movement_created_at_start_of_current_month_can_be_removed() {
    final LocalDateTime firstDayOfMonth =
        LocalDateTime.now().withDayOfMonth(1).withHour(0).withMinute(0).withSecond(0);

    final MovementDTO movement =
        MovementDTO.builder()
            .id(MovementId.generate())
            .productId(ProductId.generate())
            .movementType(MovementType.DEPOSIT)
            .movementAmount(new BigDecimal("100.00"))
            .movementDate(LocalDate.now())
            .balanceSnapshot(new BigDecimal("1000.00"))
            .createdAt(firstDayOfMonth)
            .description("Movement at start of month")
            .build();

    assertTrue(movement.canBeRemoved());
  }

  @Test
  void movement_created_at_end_of_current_month_can_be_removed() {
    final LocalDateTime lastDayOfMonth =
        LocalDateTime.now()
            .withDayOfMonth(LocalDateTime.now().toLocalDate().lengthOfMonth())
            .withHour(23)
            .withMinute(59)
            .withSecond(59);

    final MovementDTO movement =
        MovementDTO.builder()
            .id(MovementId.generate())
            .productId(ProductId.generate())
            .movementType(MovementType.WITHDRAWAL)
            .movementAmount(new BigDecimal("200.00"))
            .movementDate(LocalDate.now())
            .balanceSnapshot(new BigDecimal("800.00"))
            .createdAt(lastDayOfMonth)
            .description("Movement at end of month")
            .build();

    assertTrue(movement.canBeRemoved());
  }
}

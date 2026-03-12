package com.jbh.finance.test.application.feature.movement.policy;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.jbh.finance.application.feature.movement.dto.MovementDTO;
import com.jbh.finance.application.feature.movement.policy.MovementRemovalPolicy;
import com.jbh.finance.domain.movement.vo.MovementId;
import com.jbh.finance.domain.movement.vo.MovementType;
import com.jbh.finance.domain.product.vo.ProductId;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class MovementRemovalPolicyTest {

  private MovementRemovalPolicy policy;

  @BeforeEach
  void setUp() {
    policy = new MovementRemovalPolicy();
  }

  @Test
  void shouldAllowRemovalForMovementCreatedInCurrentMonth() {
    final MovementDTO movement =
        MovementDTO.builder()
            .id(MovementId.generate())
            .productId(ProductId.generate())
            .movementType(MovementType.DEPOSIT)
            .movementAmount(new BigDecimal("100.00"))
            .movementDate(LocalDate.now())
            .balanceSnapshot(new BigDecimal("1000.00"))
            .createdAt(LocalDateTime.now())
            .description("Current month movement")
            .build();

    assertTrue(policy.canBeRemoved(movement));
  }

  @Test
  void shouldNotAllowRemovalForMovementCreatedInPreviousMonth() {
    final MovementDTO movement =
        MovementDTO.builder()
            .id(MovementId.generate())
            .productId(ProductId.generate())
            .movementType(MovementType.WITHDRAWAL)
            .movementAmount(new BigDecimal("50.00"))
            .movementDate(LocalDate.now())
            .balanceSnapshot(new BigDecimal("950.00"))
            .createdAt(LocalDateTime.now().minusMonths(1))
            .description("Previous month movement")
            .build();

    assertFalse(policy.canBeRemoved(movement));
  }

  @Test
  void shouldNotAllowRemovalForMovementWithNullCreatedAt() {
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

    assertFalse(policy.canBeRemoved(movement));
  }

  @Test
  void shouldAllowRemovalForMovementCreatedAtStartOfCurrentMonth() {
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
            .description("Start of month movement")
            .build();

    assertTrue(policy.canBeRemoved(movement));
  }

  @Test
  void shouldAllowRemovalForMovementCreatedAtEndOfCurrentMonth() {
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
            .description("End of month movement")
            .build();

    assertTrue(policy.canBeRemoved(movement));
  }

  @Test
  void shouldNotAllowRemovalForMovementCreatedSeveralMonthsAgo() {
    final MovementDTO movement =
        MovementDTO.builder()
            .id(MovementId.generate())
            .productId(ProductId.generate())
            .movementType(MovementType.DEPOSIT)
            .movementAmount(new BigDecimal("500.00"))
            .movementDate(LocalDate.now())
            .balanceSnapshot(new BigDecimal("1500.00"))
            .createdAt(LocalDateTime.now().minusMonths(6))
            .description("Old movement")
            .build();

    assertFalse(policy.canBeRemoved(movement));
  }
}

package com.jbh.finance.application.feature.movement.dto;

import com.jbh.finance.domain.movement.vo.MovementCategoryVO;
import com.jbh.finance.domain.movement.vo.MovementId;
import com.jbh.finance.domain.movement.vo.MovementMetadata;
import com.jbh.finance.domain.movement.vo.MovementType;
import com.jbh.finance.domain.product.vo.ProductId;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import lombok.Builder;

@Builder
public record MovementDTO(
    MovementId id,
    ProductId accountId,
    MovementType movementType,
    MovementCategoryVO category,
    BigDecimal movementAmount,
    LocalDate movementDate,
    BigDecimal balanceSnapshot,
    MovementMetadata metadata,
    LocalDateTime createdAt,
    String description) {

  @Override
  public String toString() {
    return "MovementDTO{"
        + "movementType="
        + movementType
        + ", movementAmount="
        + movementAmount
        + ", movementDate="
        + movementDate
        + ", balanceSnapshot="
        + balanceSnapshot
        + '}';
  }

  public boolean isWithdrawalType() {
    return movementType.isWithdrawal();
  }

  public boolean isBalanceSnapshot() {
    return movementType.isBalanceSnapshot();
  }
}

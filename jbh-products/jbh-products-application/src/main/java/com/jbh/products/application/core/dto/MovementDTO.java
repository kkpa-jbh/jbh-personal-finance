package com.jbh.products.application.core.dto;

import com.jbh.products.domain.vo.AccountMovementMetadata;
import com.jbh.products.domain.vo.MovementCategoryVO;
import com.jbh.products.domain.vo.MovementId;
import com.jbh.products.domain.vo.MovementType;
import com.jbh.products.domain.vo.ProductId;
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
    AccountMovementMetadata metadata,
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
    return movementType == MovementType.WITHDRAWAL;
  }

  public boolean isDepositType() {
    return movementType == MovementType.DEPOSIT;
  }

  public boolean isBalanceSnapshot() {
    return movementType == MovementType.BALANCE_SNAPSHOT;
  }
}

package com.jbh.account.application.core.dto;

import com.jbh.account.domain.vo.ProductId;
import com.jbh.account.domain.vo.AccountMovementId;
import com.jbh.account.domain.vo.AccountMovementMetadata;
import com.jbh.account.domain.vo.MovementCategoryDTO;
import com.jbh.account.domain.vo.MovementType;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import lombok.Builder;

@Builder
public record MovementDTO(
    AccountMovementId id,
    ProductId accountId,
    MovementType movementType,
    MovementCategoryDTO category,
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

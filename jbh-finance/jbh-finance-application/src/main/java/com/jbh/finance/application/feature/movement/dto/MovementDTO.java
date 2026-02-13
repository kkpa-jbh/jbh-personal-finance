package com.jbh.finance.application.feature.movement.dto;

import com.jbh.finance.domain.movement.vo.MovementCategoryVO;
import com.jbh.finance.domain.movement.vo.MovementId;
import com.jbh.finance.domain.movement.vo.MovementMetadata;
import com.jbh.finance.domain.movement.vo.MovementType;
import com.jbh.finance.domain.product.vo.ProductId;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;

@Builder
@AllArgsConstructor
@SuppressWarnings("PMD.AvoidFieldNameMatchingMethodName")
public class MovementDTO {

  private final MovementId id;
  private final ProductId accountId;
  private final MovementType movementType;
  private final MovementCategoryVO category;
  private final BigDecimal movementAmount;
  private final LocalDate movementDate;
  private final BigDecimal balanceSnapshot;
  private final MovementMetadata metadata;
  private final LocalDateTime createdAt;
  private final String description;

  // mutable field we want to expose/set during tests or runtime
  private boolean toRemoval;

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

  // Provide record-like accessors so existing callers that expect id() work unchanged
  public MovementId id() {
    return id;
  }

  public ProductId accountId() {
    return accountId;
  }

  public MovementType movementType() {
    return movementType;
  }

  public MovementCategoryVO category() {
    return category;
  }

  public BigDecimal movementAmount() {
    return movementAmount;
  }

  public LocalDate movementDate() {
    return movementDate;
  }

  public BigDecimal balanceSnapshot() {
    return balanceSnapshot;
  }

  public MovementMetadata metadata() {
    return metadata;
  }

  public LocalDateTime createdAt() {
    return createdAt;
  }

  public String description() {
    return description;
  }

  public boolean isWithdrawalType() {
    return movementType.isWithdrawal();
  }

  public boolean isBalanceSnapshot() {
    return movementType.isBalanceSnapshot();
  }

  public boolean isToRemoval() {
    return toRemoval;
  }

  public void setToRemoval(final boolean toRemoval) {
    this.toRemoval = toRemoval;
  }
}

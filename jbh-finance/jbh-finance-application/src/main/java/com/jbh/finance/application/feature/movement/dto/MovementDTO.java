package com.jbh.finance.application.feature.movement.dto;

import com.jbh.commons.util.JbhMoneyUtils;
import com.jbh.commons.util.JbhStringUtils;
import com.jbh.finance.application.feature.category.dto.CategoryDTO;
import com.jbh.finance.domain.movement.vo.MovementId;
import com.jbh.finance.domain.movement.vo.MovementMetadata;
import com.jbh.finance.domain.movement.vo.MovementType;
import com.jbh.finance.domain.product.vo.ProductId;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import lombok.AllArgsConstructor;
import lombok.Builder;

@Builder
@AllArgsConstructor
@SuppressWarnings("PMD.AvoidFieldNameMatchingMethodName")
public class MovementDTO {

  private final MovementId id;
  private final ProductId productId;
  private final MovementType movementType;
  private final CategoryDTO category;
  private final BigDecimal movementAmount;
  private final LocalDate movementDate;
  private final BigDecimal balanceSnapshot;
  private final MovementMetadata metadata;
  private final LocalDateTime createdAt;
  private final String description;

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

  public ProductId productId() {
    return productId;
  }

  public MovementType movementType() {
    return movementType;
  }

  public CategoryDTO category() {
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

  /**
   * Determines if this movement can be removed. A movement can only be removed if it was created in
   * the current month, or if it was created today.
   *
   * @return true if removable, false otherwise
   */
  public boolean canBeRemoved() {
    if (createdAt == null) {
      return false;
    }

    final YearMonth currentMonth = YearMonth.now();
    final YearMonth createdMonth = YearMonth.from(createdAt);

    return currentMonth.equals(createdMonth) || LocalDate.now().equals(createdAt.toLocalDate());
  }

  public String internalMessage() {
    if (isBalanceSnapshot() || JbhMoneyUtils.isNotZero(balanceSnapshot)) {
      return JbhStringUtils.buildJsonMessage(
          "The movement contains an updated to the balance snapshot, be sure to update it later",
          "El movimiento contiene un ajuste al saldo, por favor, actualizalo más tarde");
    }
    return null;
  }

  public boolean isBalanceSnapshot() {
    return movementType.isBalanceSnapshot();
  }
}

package com.jbh.account.infra.adapters.out.persistence.movement;

import com.jbh.account.domain.vo.AccountMovementDTO;
import com.jbh.account.domain.vo.MovementType;
import io.hypersistence.utils.hibernate.type.json.JsonBinaryType;
import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.Type;

@Entity
@Getter
@Setter
@Table(name = "account_movements", schema = "acctmgmt")
public class AccountMovementJPAEntity extends PanacheEntityBase {
  // Auto generated ID
  @Id
  @Column(name = "id")
  public UUID id;

  @Column(name = "account_id")
  public UUID accountId;

  @Enumerated(EnumType.STRING)
  @Column(name = "movement_type")
  public MovementType movementType;

  @Column(name = "movement_amount", precision = 20, scale = 2)
  public BigDecimal movementAmount;

  @Column(name = "movement_date")
  public LocalDate movementDate;

  @Column(name = "balance_snapshot", precision = 20, scale = 2)
  public BigDecimal balanceSnapshot;

  @Column(name = "description")
  public String description;

  @Type(JsonBinaryType.class)
  @Column(name = "metadata", columnDefinition = "jsonb")
  public Map<String, Object> metadata;

  @Column(name = "created_at", nullable = false, updatable = false)
  public LocalDateTime createdAt;

  public static AccountMovementJPAEntity of(final AccountMovementDTO accountMovement) {
    final AccountMovementJPAEntity entity = new AccountMovementJPAEntity();
    entity.setId(accountMovement.getId() != null ? accountMovement.getId().value() : null);
    entity.setAccountId(
        accountMovement.getAccountId() != null ? accountMovement.getAccountId().value() : null);
    entity.setMovementType(accountMovement.getMovementType());
    entity.setMovementAmount(accountMovement.getMovementAmount());
    entity.setMovementDate(accountMovement.getMovementDate());
    entity.setBalanceSnapshot(accountMovement.getBalanceSnapshot());
    entity.setDescription(accountMovement.getDescription());
    entity.setMetadata(accountMovement.getMetadata());
    return entity;
  }

  @PrePersist
  protected void onCreate() {
    createdAt = LocalDateTime.now();
  }
}

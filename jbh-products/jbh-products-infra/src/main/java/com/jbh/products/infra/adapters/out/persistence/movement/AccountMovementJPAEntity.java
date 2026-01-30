package com.jbh.products.infra.adapters.out.persistence.movement;

import com.jbh.products.application.core.dto.MovementDTO;
import com.jbh.products.domain.vo.AccountMovementId;
import com.jbh.products.domain.vo.AccountMovementMetadata;
import com.jbh.products.domain.vo.AccountMovementMetadataKey;
import com.jbh.products.domain.vo.MovementCategoryDTO;
import com.jbh.products.domain.vo.MovementType;
import com.jbh.products.domain.vo.ProductId;
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
@Table(name = "account_movements", schema = "productmgmt")
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

  @Column(name = "category_type")
  public String category;

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
  public Map<AccountMovementMetadataKey, Object> metadata;

  @Column(name = "created_at", nullable = false, updatable = false)
  public LocalDateTime createdAt;

  public static AccountMovementJPAEntity toEntity(final MovementDTO accountMovement) {
    final AccountMovementJPAEntity entity = new AccountMovementJPAEntity();
    entity.setId(accountMovement.id() != null ? accountMovement.id().value() : null);
    entity.setAccountId(
        accountMovement.accountId() != null ? accountMovement.accountId().value() : null);
    entity.setMovementType(accountMovement.movementType());
    entity.setMovementAmount(accountMovement.movementAmount());
    entity.setMovementDate(accountMovement.movementDate());
    entity.setBalanceSnapshot(accountMovement.balanceSnapshot());
    entity.setMetadata(
        accountMovement.metadata() != null ? accountMovement.metadata().asMap() : null);
    entity.setCategory(accountMovement.category().getType().getTypeName());
    entity.setDescription(accountMovement.description());
    entity.setCreatedAt(accountMovement.createdAt());
    return entity;
  }

  @PrePersist
  protected void onCreate() {
    createdAt = LocalDateTime.now();
  }

  public MovementDTO toDTO() {
    return MovementDTO.builder()
        .id(AccountMovementId.of(id))
        .accountId(ProductId.of(accountId))
        .movementType(movementType)
        .category(MovementCategoryDTO.withName(movementType, category))
        .movementAmount(movementAmount)
        .movementDate(movementDate)
        .balanceSnapshot(balanceSnapshot)
        .metadata(metadata != null ? AccountMovementMetadata.of(metadata) : null)
        .createdAt(createdAt)
        .description(description)
        .build();
  }
}

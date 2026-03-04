package com.jbh.finance.infra.adapters.out.persistence.movement;

import com.jbh.finance.application.feature.movement.dto.MovementDTO;
import com.jbh.finance.domain.movement.vo.MovementId;
import com.jbh.finance.domain.movement.vo.MovementMetadata;
import com.jbh.finance.domain.movement.vo.MovementMetadataKey;
import com.jbh.finance.domain.movement.vo.MovementType;
import com.jbh.finance.domain.product.vo.ProductId;
import com.jbh.finance.infra.adapters.out.persistence.category.CategoryJPAEntity;
import io.hypersistence.utils.hibernate.type.json.JsonBinaryType;
import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
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
@Table(name = "movements", schema = "finance")
public class MovementJPAEntity extends PanacheEntityBase {
  // Auto generated ID
  @Id
  @Column(name = "id")
  public UUID id;

  @Column(name = "product_id")
  public UUID productId;

  @Enumerated(EnumType.STRING)
  @Column(name = "movement_type")
  public MovementType movementType;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "category_id")
  public CategoryJPAEntity category;

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
  public Map<MovementMetadataKey, Object> metadata;

  @Column(name = "created_at", nullable = false, updatable = false)
  public LocalDateTime createdAt;

  public static MovementJPAEntity toEntity(final MovementDTO productMovement) {
    final MovementJPAEntity entity = new MovementJPAEntity();
    entity.setId(productMovement.id() != null ? productMovement.id().value() : null);
    entity.setProductId(
        productMovement.productId() != null ? productMovement.productId().value() : null);
    entity.setMovementType(productMovement.movementType());
    entity.setMovementAmount(productMovement.movementAmount());
    entity.setMovementDate(productMovement.movementDate());
    entity.setBalanceSnapshot(productMovement.balanceSnapshot());
    entity.setMetadata(
        productMovement.metadata() != null ? productMovement.metadata().asMap() : null);
    entity.setDescription(productMovement.description());
    entity.setCreatedAt(productMovement.createdAt());
    entity.setDescription(productMovement.description());
    return entity;
  }

  @PrePersist
  protected void onCreate() {
    createdAt = LocalDateTime.now();
  }

  public MovementDTO toDTO() {
    return MovementDTO.builder()
        .id(MovementId.of(id))
        .productId(ProductId.of(productId))
        .movementType(movementType)
        .category(category != null ? category.toDTO() : null)
        .movementAmount(movementAmount)
        .movementDate(movementDate)
        .balanceSnapshot(balanceSnapshot)
        .metadata(metadata != null ? MovementMetadata.of(metadata) : null)
        .createdAt(createdAt)
        .description(description)
        .build();
  }
}

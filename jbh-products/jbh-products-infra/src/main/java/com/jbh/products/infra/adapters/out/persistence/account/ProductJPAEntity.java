package com.jbh.products.infra.adapters.out.persistence.account;

import com.jbh.products.application.core.dto.ProductDTO;
import com.jbh.products.domain.product.vo.ProductId;
import com.jbh.products.domain.product.vo.ProductMetadata;
import com.jbh.products.domain.product.vo.ProductMetadataKey;
import com.jbh.products.domain.product.vo.ProductType;
import io.hypersistence.utils.hibernate.type.json.JsonBinaryType;
import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.Type;

@Entity
@Getter
@Setter
@Table(name = "products", schema = "finance")
public class ProductJPAEntity extends PanacheEntityBase {

  @Id
  @Column(name = "id")
  private UUID id;

  @Column(name = "name", nullable = false)
  private String name;

  @Column(name = "is_active", nullable = false)
  private Boolean isActive = true;

  @Enumerated(EnumType.STRING)
  @Column(name = "type")
  private ProductType type;

  @Column(name = "user_id", nullable = false)
  private UUID userId;

  @Column(name = "movement_balance", nullable = false, precision = 20, scale = 2)
  private BigDecimal movementBalance = BigDecimal.ZERO;

  @Column(name = "current_balance", nullable = false, precision = 20, scale = 2)
  private BigDecimal currentBalance = BigDecimal.ZERO;

  @Column(name = "net_profit_balance", nullable = false, precision = 20, scale = 2)
  private BigDecimal netProfitBalance = BigDecimal.ZERO;

  @Column(name = "net_growth_rate", nullable = false, precision = 20, scale = 2)
  private BigDecimal netGrowthRate = BigDecimal.ZERO;

  @Type(JsonBinaryType.class)
  @Column(name = "metadata", columnDefinition = "jsonb")
  private Map<ProductMetadataKey, Object> metadata;

  @Column(name = "created_at", nullable = false, updatable = false)
  private LocalDateTime createdAt;

  @Column(name = "updated_at", nullable = false)
  private LocalDateTime updatedAt;

  public static ProductJPAEntity toEntity(final ProductDTO account) {
    final ProductJPAEntity accountJpaEntity = new ProductJPAEntity();
    accountJpaEntity.setId(account.id() != null ? account.id().value() : null);
    accountJpaEntity.setName(account.name());
    accountJpaEntity.setType(account.type());
    accountJpaEntity.setUserId(account.userId());
    accountJpaEntity.setMovementBalance(account.movementBalance());
    accountJpaEntity.setCurrentBalance(account.currentBalance());
    accountJpaEntity.setNetProfitBalance(account.netProfitBalance());
    accountJpaEntity.setIsActive(account.isActive());
    accountJpaEntity.setNetGrowthRate(account.netGrowthRate());
    accountJpaEntity.setMetadata(account.metadata().getData());
    accountJpaEntity.setCreatedAt(account.createdAt());
    accountJpaEntity.setUpdatedAt(account.updatedAt());
    return accountJpaEntity;
  }

  @PrePersist
  protected void onCreate() {
    createdAt = LocalDateTime.now();
    updatedAt = LocalDateTime.now();
  }

  @PreUpdate
  protected void onUpdate() {
    updatedAt = LocalDateTime.now();
  }

  public ProductDTO toDTO() {
    return ProductDTO.defaultBuilder(userId, ProductId.of(id), name, type)
        .movementBalance(movementBalance)
        .currentBalance(currentBalance)
        .netProfitBalance(netProfitBalance)
        .isActive(isActive)
        .netGrowthRate(netGrowthRate)
        .metadata(ProductMetadata.fromMap(metadata))
        .createdAt(createdAt)
        .updatedAt(updatedAt)
        .build();
  }
}

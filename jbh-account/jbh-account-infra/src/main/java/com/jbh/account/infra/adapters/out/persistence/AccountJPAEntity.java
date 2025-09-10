package com.jbh.account.infra.adapters.out.persistence;

import com.jbh.account.domain.vo.AccountDomainDTO;
import com.jbh.account.domain.vo.AccountId;
import com.jbh.account.domain.vo.AccountType;
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
@Table(name = "accounts", schema = "acctmgmt")
public class AccountJPAEntity extends PanacheEntityBase {

  @Id
  @Column(name = "id")
  public UUID id;

  @Column(name = "name", nullable = false)
  public String name;

  @Column(name = "is_active", nullable = false)
  public Boolean isActive = true;

  @Enumerated(EnumType.STRING)
  @Column(name = "type")
  public AccountType type;

  @Column(name = "user_id", nullable = false)
  public UUID userId;

  @Column(name = "movement_balance", nullable = false, precision = 20, scale = 2)
  public BigDecimal movementBalance = BigDecimal.ZERO;

  @Column(name = "current_balance", nullable = false, precision = 20, scale = 2)
  public BigDecimal currentBalance = BigDecimal.ZERO;

  @Column(name = "profit_balance", nullable = false, precision = 20, scale = 2)
  public BigDecimal profitBalance = BigDecimal.ZERO;

  @Type(JsonBinaryType.class)
  @Column(name = "metadata", columnDefinition = "jsonb")
  public Map<String, Object> metadata;

  @Column(name = "created_at", nullable = false, updatable = false)
  public LocalDateTime createdAt;

  @Column(name = "updated_at", nullable = false)
  public LocalDateTime updatedAt;

  public static AccountJPAEntity of(final AccountDomainDTO account) {
    final AccountJPAEntity accountJpaEntity = new AccountJPAEntity();
    accountJpaEntity.setId(account.getId() != null ? account.getId().value() : null);
    accountJpaEntity.setName(account.getName());
    accountJpaEntity.setType(account.getType());
    accountJpaEntity.setUserId(account.getUserId());
    accountJpaEntity.setMovementBalance(account.getMovementBalance());
    accountJpaEntity.setCurrentBalance(account.getCurrentBalance());
    accountJpaEntity.setProfitBalance(account.getProfitBalance());
    accountJpaEntity.setCreatedAt(account.getCreatedAt());
    accountJpaEntity.setUpdatedAt(account.getUpdatedAt());
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

  public AccountDomainDTO toDTO() {
    return AccountDomainDTO.builder()
        .id(AccountId.of(id))
        .name(name)
        .type(type)
        .userId(userId)
        .movementBalance(movementBalance)
        .currentBalance(currentBalance)
        .profitBalance(profitBalance)
        .createdAt(createdAt)
        .updatedAt(updatedAt)
        .build();
  }
}
package com.jbh.account_infra.persistence;

import io.quarkus.hibernate.orm.panache.PanacheEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "accounts", schema = "acctmgmt")
public class AccountJPAEntity extends PanacheEntity {

  @Column(name = "account_number", nullable = false, unique = true)
  public String accountNumber;

  @Column(name = "account_name", nullable = false)
  public String accountName;

  @Column(name = "account_type", nullable = false)
  public String accountType;

  @Column(name = "balance", precision = 19, scale = 2)
  public BigDecimal balance = BigDecimal.ZERO;

  @Column(name = "currency", length = 3)
  public String currency = "USD";

  @Column(name = "is_active")
  public Boolean isActive = true;

  @Column(name = "created_at")
  public LocalDateTime createdAt;

  @Column(name = "updated_at")
  public LocalDateTime updatedAt;

  @PrePersist
  public void prePersist() {
    createdAt = LocalDateTime.now();
    updatedAt = LocalDateTime.now();
  }

  @PreUpdate
  public void preUpdate() {
    updatedAt = LocalDateTime.now();
  }
}
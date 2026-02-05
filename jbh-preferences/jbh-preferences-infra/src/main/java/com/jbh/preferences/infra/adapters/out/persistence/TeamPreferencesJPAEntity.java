package com.jbh.preferences.infra.adapters.out.persistence;

import com.jbh.preferences.application.core.dto.TeamPreferencesDTO;
import com.jbh.preferences.domain.vo.Currency;
import com.jbh.preferences.domain.vo.PreferencesMetadata;
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
@Table(name = "team_preferences", schema = "preferences")
public class TeamPreferencesJPAEntity extends PanacheEntityBase {

  @Id
  @Column(name = "team_id")
  private UUID teamId;

  @Enumerated(EnumType.STRING)
  @Column(name = "default_currency", nullable = false, length = 10)
  private Currency defaultCurrency = Currency.COP;

  @Column(name = "savings_goal", precision = 20, scale = 2)
  private BigDecimal savingsGoal = BigDecimal.ZERO;

  @Type(JsonBinaryType.class)
  @Column(name = "metadata", columnDefinition = "jsonb")
  private Map<String, Object> metadata;

  @Column(name = "last_modified_by")
  private UUID lastModifiedBy;

  @Column(name = "created_at", nullable = false, updatable = false)
  private LocalDateTime createdAt;

  @Column(name = "updated_at", nullable = false)
  private LocalDateTime updatedAt;

  public static TeamPreferencesJPAEntity toEntity(final TeamPreferencesDTO dto) {
    final TeamPreferencesJPAEntity entity = new TeamPreferencesJPAEntity();
    entity.setTeamId(dto.teamId());
    entity.setDefaultCurrency(
        dto.defaultCurrency() != null ? dto.defaultCurrency() : Currency.defaultCurrency());
    entity.setSavingsGoal(dto.savingsGoal());
    entity.setMetadata(dto.metadata() != null ? dto.metadata().getData() : null);
    entity.setLastModifiedBy(dto.lastModifiedBy());
    entity.setCreatedAt(dto.createdAt());
    entity.setUpdatedAt(dto.updatedAt());
    return entity;
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

  public TeamPreferencesDTO toDTO() {
    return TeamPreferencesDTO.internalBuilder()
        .teamId(teamId)
        .defaultCurrency(defaultCurrency)
        .savingsGoal(savingsGoal)
        .metadata(
            metadata != null ? PreferencesMetadata.fromMap(metadata) : PreferencesMetadata.empty())
        .lastModifiedBy(lastModifiedBy)
        .createdAt(createdAt)
        .updatedAt(updatedAt)
        .build();
  }
}

package com.jbh.preferences.infra.adapters.out.persistence;

import com.jbh.preferences.application.core.dto.UserPreferencesDTO;
import com.jbh.preferences.domain.vo.Currency;
import com.jbh.preferences.domain.vo.Language;
import com.jbh.preferences.domain.vo.PreferencesId;
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
@Table(name = "user_preferences", schema = "userprefs")
public class UserPreferencesJPAEntity extends PanacheEntityBase {

  @Id
  @Column(name = "id")
  private UUID id;

  @Column(name = "user_id", nullable = false, unique = true)
  private UUID userId;

  @Column(name = "default_lang", nullable = false, length = 10)
  private String defaultLang = "en";

  @Enumerated(EnumType.STRING)
  @Column(name = "default_currency", nullable = false, length = 10)
  private Currency defaultCurrency = Currency.defaultCurrency();

  @Column(name = "savings_goal", precision = 20, scale = 2)
  private BigDecimal savingsGoal = BigDecimal.ZERO;

  @Column(name = "default_product_id")
  private UUID defaultProductId;

  @Type(JsonBinaryType.class)
  @Column(name = "metadata", columnDefinition = "jsonb")
  private Map<String, Object> metadata;

  @Column(name = "created_at", nullable = false, updatable = false)
  private LocalDateTime createdAt;

  @Column(name = "updated_at", nullable = false)
  private LocalDateTime updatedAt;

  public static UserPreferencesJPAEntity toEntity(final UserPreferencesDTO dto) {
    final UserPreferencesJPAEntity entity = new UserPreferencesJPAEntity();
    entity.setId(dto.id() != null ? dto.id().value() : null);
    entity.setUserId(dto.userId());
    entity.setDefaultLang(dto.defaultLang() != null ? dto.defaultLang().code() : "en");
    entity.setDefaultCurrency(
        dto.defaultCurrency() != null ? dto.defaultCurrency() : Currency.defaultCurrency());
    entity.setSavingsGoal(dto.savingsGoal());
    entity.setDefaultProductId(dto.defaultAccountId());
    entity.setMetadata(dto.metadata() != null ? dto.metadata().getData() : null);
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

  public UserPreferencesDTO toDTO() {
    return UserPreferencesDTO.internalBuilder()
        .id(PreferencesId.of(id))
        .userId(userId)
        .defaultLang(Language.of(defaultLang))
        .defaultCurrency(defaultCurrency)
        .savingsGoal(savingsGoal)
        .defaultAccountId(defaultProductId)
        .metadata(
            metadata != null ? PreferencesMetadata.fromMap(metadata) : PreferencesMetadata.empty())
        .createdAt(createdAt)
        .updatedAt(updatedAt)
        .build();
  }
}

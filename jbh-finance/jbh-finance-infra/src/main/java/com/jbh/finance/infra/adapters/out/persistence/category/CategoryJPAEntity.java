package com.jbh.finance.infra.adapters.out.persistence.category;

import com.jbh.finance.application.feature.category.dto.CategoryDTO;
import com.jbh.finance.domain.category.vo.CategorySourceVO;
import com.jbh.finance.domain.category.vo.CategoryTypeVO;
import io.hypersistence.utils.hibernate.type.json.JsonBinaryType;
import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import java.util.Map;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.Type;

@Entity
@Getter
@Setter
@Table(name = "categories", schema = "finance")
@SuppressWarnings("PMD.UnnecessaryAnnotationValueElement")
public class CategoryJPAEntity extends PanacheEntityBase {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "id")
  public Long id;

  @Column(name = "source", nullable = false)
  @Enumerated(value = EnumType.STRING)
  public CategorySourceVO source;

  @Column(name = "alias", nullable = false, length = 50)
  public String alias;

  @Column(name = "active", nullable = false)
  public boolean active;

  @Type(JsonBinaryType.class)
  @Column(name = "display_name", columnDefinition = "jsonb")
  public Map<String, String> displayName;

  @Column(name = "created_at", nullable = false, updatable = false)
  public LocalDateTime createdAt;

  @Column(name = "updated_at", nullable = false)
  public LocalDateTime updatedAt;

  public static CategoryJPAEntity fromDTO(final CategoryDTO dto) {
    final var entity = new CategoryJPAEntity();

    entity.setId(dto.getCategoryId());
    entity.setActive(dto.isActive());
    entity.setAlias(dto.getAlias());
    entity.setDisplayName(dto.getDisplayName());
    entity.setSource(dto.getSource());

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

  public CategoryDTO toDTO() {
    return CategoryDTO.withEntity(toCategoryType(), id, displayName, active);
  }

  private CategoryTypeVO toCategoryType() {
    return new CategoryTypeVO(source, alias);
  }
}

package com.jbh.finance.infra.adapters.in.rest.category.response;

import com.jbh.finance.infra.adapters.out.persistence.category.CategoryJPAEntity;
import java.util.Map;

public record CategoryEntityResponse(
    Long id, String source, String alias, boolean active, Map<String, String> displayName) {

  public static CategoryEntityResponse fromEntity(final CategoryJPAEntity entity) {
    return new CategoryEntityResponse(
        entity.getId(),
        entity.getSource().name(),
        entity.getAlias(),
        entity.isActive(),
        entity.getDisplayName());
  }
}

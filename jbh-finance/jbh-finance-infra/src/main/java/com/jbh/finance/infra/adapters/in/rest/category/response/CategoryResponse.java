package com.jbh.finance.infra.adapters.in.rest.category.response;

import com.jbh.finance.application.feature.category.dto.CategoryDTO;
import com.jbh.finance.domain.category.vo.CategorySourceVO;
import java.util.Map;

public record CategoryResponse(
    String alias, Map<String, String> translationKey, CategorySourceVO source) {
  public static CategoryResponse fromDTO(final CategoryDTO dto) {
    return new CategoryResponse(dto.getAlias(), dto.getDisplayName(), dto.getSource());
  }
}

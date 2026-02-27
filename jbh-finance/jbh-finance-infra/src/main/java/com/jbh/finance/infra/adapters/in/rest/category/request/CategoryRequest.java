package com.jbh.finance.infra.adapters.in.rest.category.request;

import com.jbh.finance.application.feature.category.dto.CategoryDTO;
import com.jbh.finance.domain.category.vo.CategoryTypeVO;

public record CategoryRequest(Long categoryId, CategoryTypeVO categoryType) {

  public CategoryDTO toDTO() {
    return CategoryDTO.withInternalPurpose(categoryType, categoryId);
  }
}

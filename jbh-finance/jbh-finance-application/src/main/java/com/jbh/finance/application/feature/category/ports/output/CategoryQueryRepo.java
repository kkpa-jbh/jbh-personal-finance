package com.jbh.finance.application.feature.category.ports.output;

import com.jbh.finance.application.feature.category.dto.CategoryDTO;
import com.jbh.finance.domain.category.vo.CategorySourceVO;
import java.util.List;

public interface CategoryQueryRepo {

  List<CategoryDTO> findAllSystemCategories();

  List<CategoryDTO> findActiveBySource(CategorySourceVO source);
}

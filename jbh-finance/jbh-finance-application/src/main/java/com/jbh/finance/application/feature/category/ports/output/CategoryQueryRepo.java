package com.jbh.finance.application.feature.category.ports.output;

import com.jbh.finance.application.feature.category.dto.CategoryDTO;
import java.util.List;

public interface CategoryQueryRepo {

  List<CategoryDTO> findAllSystemCategories();
}

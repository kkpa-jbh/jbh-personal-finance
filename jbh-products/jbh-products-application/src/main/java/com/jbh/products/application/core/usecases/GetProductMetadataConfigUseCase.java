package com.jbh.products.application.core.usecases;

import com.jbh.products.application.core.dto.MetadataFieldConfigDTO;
import com.jbh.products.domain.vo.ProductType;
import java.util.List;

public interface GetProductMetadataConfigUseCase {

  List<MetadataFieldConfigDTO> execute(ProductType productType);
}

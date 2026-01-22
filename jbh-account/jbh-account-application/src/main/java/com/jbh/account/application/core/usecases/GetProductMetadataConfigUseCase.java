package com.jbh.account.application.core.usecases;

import com.jbh.account.application.core.dto.MetadataFieldConfigDTO;
import com.jbh.account.domain.vo.ProductType;
import java.util.List;

public interface GetProductMetadataConfigUseCase {

  List<MetadataFieldConfigDTO> execute(ProductType productType);
}

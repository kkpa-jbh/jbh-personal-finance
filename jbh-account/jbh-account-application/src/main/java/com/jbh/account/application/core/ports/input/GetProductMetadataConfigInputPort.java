package com.jbh.account.application.core.ports.input;

import com.jbh.account.application.core.dto.MetadataFieldConfigDTO;
import com.jbh.account.application.core.services.metadata.ProductMetadataConfigRegistry;
import com.jbh.account.application.core.usecases.GetProductMetadataConfigUseCase;
import com.jbh.account.domain.vo.ProductType;
import java.util.List;

public class GetProductMetadataConfigInputPort implements GetProductMetadataConfigUseCase {

  private final ProductMetadataConfigRegistry registry;

  public GetProductMetadataConfigInputPort(final ProductMetadataConfigRegistry registry) {
    this.registry = registry;
  }

  @Override
  public List<MetadataFieldConfigDTO> execute(final ProductType productType) {
    if (productType == null) {
      throw new IllegalArgumentException("ProductType cannot be null");
    }
    return registry.getConfigurationFor(productType);
  }
}

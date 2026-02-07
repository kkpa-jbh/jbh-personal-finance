package com.jbh.products.application.feature.product.ports.input;

import com.jbh.products.application.feature.product.dto.MetadataFieldConfigDTO;
import com.jbh.products.application.feature.product.services.metadata.ProductMetadataConfigRegistry;
import com.jbh.products.application.feature.product.usecases.GetProductMetadataConfigUseCase;
import com.jbh.products.domain.product.vo.ProductType;
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

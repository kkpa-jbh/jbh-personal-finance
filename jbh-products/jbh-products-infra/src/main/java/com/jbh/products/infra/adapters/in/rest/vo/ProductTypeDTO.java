package com.jbh.products.infra.adapters.in.rest.vo;

import com.jbh.products.domain.vo.ProductType;
import java.util.Arrays;
import java.util.List;

public record ProductTypeDTO(String name, String translationKey) {

  public static ProductTypeDTO from(final ProductType productType) {
    return new ProductTypeDTO(productType.name(), productType.getTranslationKey());
  }

  public static List<ProductTypeDTO> allProductTypes() {
    return Arrays.stream(ProductType.values()).map(ProductTypeDTO::from).toList();
  }
}

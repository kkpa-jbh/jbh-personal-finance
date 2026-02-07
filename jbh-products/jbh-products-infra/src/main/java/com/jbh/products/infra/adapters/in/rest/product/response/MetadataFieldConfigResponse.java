package com.jbh.products.infra.adapters.in.rest.product.response;

import com.jbh.products.application.core.dto.MetadataFieldConfigDTO;

public record MetadataFieldConfigResponse(
    String key,
    String valueType,
    boolean required,
    String minValue,
    String maxValue,
    String displayName
) {
  public static MetadataFieldConfigResponse fromDTO(final MetadataFieldConfigDTO dto) {
    return new MetadataFieldConfigResponse(
        dto.key(), dto.valueType(), dto.required(),
        dto.minValue(), dto.maxValue(), dto.displayName()
    );
  }
}

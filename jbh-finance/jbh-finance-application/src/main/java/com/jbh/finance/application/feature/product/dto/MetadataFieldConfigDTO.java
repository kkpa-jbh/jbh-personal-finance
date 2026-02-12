package com.jbh.finance.application.feature.product.dto;

public record MetadataFieldConfigDTO(
    String key,
    String valueType,
    boolean required,
    String minValue,
    String maxValue,
    String displayName) {}

package com.jbh.account.application.core.dto;

public record MetadataFieldConfigDTO(
    String key,
    String valueType,
    boolean required,
    String minValue,
    String maxValue,
    String displayName) {}

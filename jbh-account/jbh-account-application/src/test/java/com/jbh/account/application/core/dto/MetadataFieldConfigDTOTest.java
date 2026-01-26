package com.jbh.account.application.core.dto;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class MetadataFieldConfigDTOTest {

  @Test
  void shouldCreateDTOWithAllFields() {
    final MetadataFieldConfigDTO dto =
        new MetadataFieldConfigDTO("TEST_KEY", "STRING", true, "0", "100", "");

    assertEquals("TEST_KEY", dto.key());
    assertEquals("STRING", dto.valueType());
    assertTrue(dto.required());
    assertEquals("0", dto.minValue());
    assertEquals("100", dto.maxValue());
  }

  @Test
  void shouldCreateDTOWithNullMinMax() {
    final MetadataFieldConfigDTO dto =
        new MetadataFieldConfigDTO("TEST_KEY", "DATE", false, null, null, "");

    assertEquals("TEST_KEY", dto.key());
    assertEquals("DATE", dto.valueType());
    assertNull(dto.minValue());
    assertNull(dto.maxValue());
  }
}

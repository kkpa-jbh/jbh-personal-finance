package com.jbh.commons.util;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Map;

public class JsonUtils {

  private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

  public static Map<String, String> jsonToMap(final String json) {
    try {
      return OBJECT_MAPPER.readValue(json, new TypeReference<Map<String, String>>() {});
    } catch (final Exception e) {
      throw new IllegalArgumentException("Invalid JSON string", e);
    }
  }
}

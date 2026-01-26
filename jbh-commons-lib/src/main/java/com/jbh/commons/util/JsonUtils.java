package com.jbh.commons.util;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Map;

public final class JsonUtils {

  private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

  private JsonUtils() {}

  public static Map<String, String> jsonToMap(final String json) {
    try {
      return OBJECT_MAPPER.readValue(json, new TypeReference<>() {});
    } catch (final JsonProcessingException e) {
      throw new IllegalArgumentException("Invalid JSON string", e);
    }
  }
}

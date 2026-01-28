package com.jbh.commons.util;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Map;

public final class JbhJsonUtils {

  private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

  private JbhJsonUtils() {}

  public static Map<String, String> jsonToMap(final String json) {
    try {
      return OBJECT_MAPPER.readValue(json, new TypeReference<>() {});
    } catch (final JsonProcessingException e) {
      throw new IllegalArgumentException("Invalid JSON string", e);
    }
  }

  @SuppressWarnings("unchecked")
  public static String getField(final String field, final String language) {
    try {
      final Map<String, Object> jsonObject =
          OBJECT_MAPPER.readValue(field, Map.class);
      final String value = (String) jsonObject.get(language);
      if (JbhStringUtils.isBlank(value)) {
        return (String) jsonObject.entrySet().iterator().next().getValue();
      }
      return value;
    } catch (final JsonProcessingException e) {
      return field;
    }
  }
}

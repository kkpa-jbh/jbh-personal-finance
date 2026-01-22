package com.jbh.account.domain.vo;

import com.jbh.account.domain.utils.JbhStringUtils;

public enum MetadataValueType {
  STRING("String", "Texto"),
  INT("Integer", "Entero"),
  BIGDECIMAL("Decimal", "Decimal"),
  DATE("Date", "Fecha"),
  BOOLEAN("Boolean", "Booleano");

  private final String translationKey;

  MetadataValueType(final String eng, final String es) {
    this.translationKey = JbhStringUtils.buildJsonMessage(eng, es);
  }

  public String getTranslationKey() {
    return translationKey;
  }
}

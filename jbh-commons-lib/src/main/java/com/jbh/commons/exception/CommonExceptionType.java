package com.jbh.commons.exception;

import com.jbh.commons.util.JbhStringUtils;

public enum CommonExceptionType implements BusinessExceptionType {
  INVALID_PERCENTAGE("Percentage must be between 0 and 100", "Porcentaje debe estar entre 0 y 100");
  ;

  private final String en;
  private final String es;

  CommonExceptionType(final String en, final String es) {
    this.en = en;
    this.es = es;
  }

  @Override
  public String getFormattedMessage(final Object... args) {
    return JbhStringUtils.buildFormattedJsonMessage(en, es, args);
  }

  @Override
  public String getMessage() {
    return JbhStringUtils.buildJsonMessage(en, es);
  }
}

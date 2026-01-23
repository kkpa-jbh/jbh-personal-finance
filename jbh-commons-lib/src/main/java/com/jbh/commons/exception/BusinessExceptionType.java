package com.jbh.commons.exception;

import java.util.Locale;

public interface BusinessExceptionType {
  @SuppressWarnings("FORMAT_STRING_MANIPULATION")
  default String getFormattedMessage(final Object... args) {
    return String.format(Locale.ROOT, getMessage(), args);
  }

  String getMessage();
}

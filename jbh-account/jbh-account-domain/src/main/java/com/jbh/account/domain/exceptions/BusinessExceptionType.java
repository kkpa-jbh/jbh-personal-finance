package com.jbh.account.domain.exceptions;

import java.util.Locale;

public interface BusinessExceptionType {
  String getMessage();

  @SuppressWarnings("FORMAT_STRING_MANIPULATION")
  default String getFormattedMessage(final Object... args) {
    return String.format(Locale.ROOT, getMessage(), args);
  }
}

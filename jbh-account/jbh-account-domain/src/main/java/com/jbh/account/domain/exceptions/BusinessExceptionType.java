package com.jbh.account.domain.exceptions;

public interface BusinessExceptionType {
  String getMessage();

  default String getFormattedMessage(final Object... args) {
    return String.format(getMessage(), args);
  }
}

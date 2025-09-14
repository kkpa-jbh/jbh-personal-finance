package com.jbh.account.infra.exceptions;

import java.io.Serial;

public class JbhAccountValidationException extends Exception {
  @Serial private static final long serialVersionUID = 1L;

  public JbhAccountValidationException(final String message) {
    super(message);
  }

  public JbhAccountValidationException(final String message, final Throwable cause) {
    super(message, cause);
  }
}

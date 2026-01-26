package com.jbh.commons.exception;

import java.io.Serial;

public class InternalSystemException extends Exception {
  @Serial private static final long serialVersionUID = -7904385600828403985L;
  private final String message;
  private String errorCode;

  public InternalSystemException(final String message) {
    super(message);
    this.message = message;
  }

  public InternalSystemException(final String message, final Throwable cause) {
    super(message, cause);
    this.message = message;
  }

  public InternalSystemException(final String message, final String errorCode) {
    super(message);
    this.errorCode = errorCode;
    this.message = message;
  }

  @SuppressWarnings("PMD.MissingOverride")
  public String getMessage() {
    return message;
  }

  public String getErrorCode() {
    return errorCode;
  }
}

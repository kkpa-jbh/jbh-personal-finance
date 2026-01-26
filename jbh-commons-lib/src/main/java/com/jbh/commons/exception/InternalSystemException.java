package com.jbh.commons.exception;

public class InternalSystemException extends Exception {

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

  public String getMessage() {
    return message;
  }

  public String getErrorCode() {
    return errorCode;
  }
}

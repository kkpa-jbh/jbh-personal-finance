package com.jbh.commons.gateway;

import com.fasterxml.jackson.annotation.JsonProperty;

public class AuthErrorResponse {
  @JsonProperty("error_code")
  private String errorCode;

  private String message;
  private long timestamp;

  public AuthErrorResponse(final String errorCode, final String message) {
    this.errorCode = errorCode;
    this.message = message;
    this.timestamp = System.currentTimeMillis();
  }

  public AuthErrorResponse(final String errorCode, final String message, final long timestamp) {
    this.errorCode = errorCode;
    this.message = message;
    this.timestamp = timestamp;
  }

  public String getErrorCode() {
    return errorCode;
  }

  public void setErrorCode(final String errorCode) {
    this.errorCode = errorCode;
  }

  public String getMessage() {
    return message;
  }

  public void setMessage(final String message) {
    this.message = message;
  }

  public long getTimestamp() {
    return timestamp;
  }

  public void setTimestamp(final long timestamp) {
    this.timestamp = timestamp;
  }

  @Override
  public int hashCode() {
    int result = errorCode.hashCode();
    result = 31 * result + message.hashCode();
    result = 31 * result + (int) (timestamp ^ (timestamp >>> 32));
    return result;
  }

  @Override
  public boolean equals(final Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }

    final AuthErrorResponse that = (AuthErrorResponse) o;

    if (timestamp != that.timestamp) {
      return false;
    }
    if (!errorCode.equals(that.errorCode)) {
      return false;
    }
    return message.equals(that.message);
  }

  @Override
  public String toString() {
    return "AuthErrorResponse{"
        + "errorCode='"
        + errorCode
        + '\''
        + ", message='"
        + message
        + '\''
        + ", timestamp="
        + timestamp
        + '}';
  }
}

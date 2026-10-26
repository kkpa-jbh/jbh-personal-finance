package com.jbh.preferences.application.core.vo;

import java.util.Objects;

/**
 * The Authorization header of the caller, as it came in. The application never reads it. It only
 * gives it to the output port that asks IAM about the caller.
 */
public record CallerToken(String value) {

  public CallerToken {
    Objects.requireNonNull(value, "Caller token cannot be null");
  }

  /** Never prints the token. */
  @Override
  public String toString() {
    return "CallerToken[****]";
  }
}

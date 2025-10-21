package com.jbh.account.domain.utils;

public final class JbhBooleanUtils {

  private JbhBooleanUtils() {}

  public static boolean isTrue(final Object value) {
    return value != null && value.equals(true);
  }
}

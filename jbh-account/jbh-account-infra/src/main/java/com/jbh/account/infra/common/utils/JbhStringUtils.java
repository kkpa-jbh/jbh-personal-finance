package com.jbh.account.infra.common.utils;

import java.util.Locale;
import org.apache.commons.lang3.StringUtils;

public final class JbhStringUtils {

  private JbhStringUtils() {
    // Utility class
  }

  public static boolean isBlank(final String str) {
    return StringUtils.isBlank(str);
  }

  public static boolean isNotBlank(final String str) {
    return StringUtils.isNotBlank(str);
  }

  public static String toLowerCase(final String str) {
    return StringUtils.lowerCase(str, Locale.ROOT);
  }
}

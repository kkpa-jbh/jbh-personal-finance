package com.jbh.account.application.exceptions;

import com.jbh.account.domain.utils.JbhStringUtils;

public record JbhExceptionMessage(String en, String es) {
  public JbhExceptionMessage {
    if (JbhStringUtils.isBlank(en) && JbhStringUtils.isBlank(es)) {
      throw new IllegalArgumentException("en and es cannot be blank");
    }
  }

  public String getMessage() {
    return JbhStringUtils.buildJsonMessage(en, es);
  }
}

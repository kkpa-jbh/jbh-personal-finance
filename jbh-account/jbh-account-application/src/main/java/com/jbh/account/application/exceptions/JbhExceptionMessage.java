package com.jbh.account.application.exceptions;

import com.jbh.account.domain.utils.JbhStringUtils;
import java.io.Serial;
import java.io.Serializable;

public record JbhExceptionMessage(String en, String es) implements Serializable {
  @Serial private static final long serialVersionUID = -7904385600828403430L;

  public JbhExceptionMessage {
    if (JbhStringUtils.isBlank(en) && JbhStringUtils.isBlank(es)) {
      throw new IllegalArgumentException("en and es cannot be blank");
    }
  }

  public String getMessage() {
    return JbhStringUtils.buildJsonMessage(en, es);
  }
}

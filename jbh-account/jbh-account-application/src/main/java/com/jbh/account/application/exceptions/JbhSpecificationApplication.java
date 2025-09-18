package com.jbh.account.application.exceptions;

import java.io.Serial;
import lombok.Getter;

@Getter
public class JbhSpecificationApplication extends Exception {

  @Serial private static final long serialVersionUID = -7904385600828409999L;

  private final String customMessage;

  public JbhSpecificationApplication(
      final String message, final Throwable cause, final String customMessage) {
    super(message, cause);
    this.customMessage = customMessage;
  }
}

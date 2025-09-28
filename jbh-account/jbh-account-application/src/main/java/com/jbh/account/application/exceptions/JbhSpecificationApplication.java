package com.jbh.account.application.exceptions;

import java.io.Serial;
import lombok.Getter;

@Getter
public class JbhSpecificationApplication extends Exception {

  @Serial private static final long serialVersionUID = -7904385600828409999L;

  private final JbhExceptionMessage customMessage;

  public JbhSpecificationApplication(
      final String message, final JbhExceptionMessage customMessage) {
    super(message);
    this.customMessage = customMessage;
  }
}

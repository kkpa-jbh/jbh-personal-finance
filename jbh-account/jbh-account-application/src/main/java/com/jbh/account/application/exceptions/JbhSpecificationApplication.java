package com.jbh.account.application.exceptions;

import lombok.Getter;

@Getter
public class JbhSpecificationApplication extends Exception {

  private final String customMessage;

  public JbhSpecificationApplication(
      final String message, final Throwable cause, final String customMessage) {
    super(message, cause);
    this.customMessage = customMessage;
  }
}

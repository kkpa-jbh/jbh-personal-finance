package com.jbh.account.domain.exceptions;

import java.io.Serial;
import lombok.Getter;

@Getter
public class AccountBusinessException extends Exception {

  @Serial private static final long serialVersionUID = -7904385600828409999L;

  private JbhExceptionMessage customMessage;
  private BusinessExceptionType businessExceptionType;

  public AccountBusinessException(final String message, final JbhExceptionMessage customMessage) {
    super(message);
    this.customMessage = customMessage;
  }

  public AccountBusinessException(final BusinessExceptionType businessExceptionType) {
    super(businessExceptionType.getMessage());
    this.businessExceptionType = businessExceptionType;
  }
}

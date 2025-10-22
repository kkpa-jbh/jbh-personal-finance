package com.jbh.account.domain.exceptions;

import lombok.Getter;

@Getter
public class AccountBusinessException extends Exception {

  // Generate serialVersionUID
  private static final long serialVersionUID = 132234234234L;

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

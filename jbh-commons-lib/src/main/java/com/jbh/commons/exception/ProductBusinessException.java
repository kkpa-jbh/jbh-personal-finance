package com.jbh.commons.exception;

public class ProductBusinessException extends Exception {

  // Generate serialVersionUID
  private static final long serialVersionUID = 132234234234L;

  private final BusinessExceptionType businessExceptionType;

  public ProductBusinessException(final BusinessExceptionType businessExceptionType) {
    super(businessExceptionType.getMessage());
    this.businessExceptionType = businessExceptionType;
  }

  public ProductBusinessException(
      final BusinessExceptionType businessExceptionType, final Object... args) {
    super(businessExceptionType.getFormattedMessage(args));
    this.businessExceptionType = businessExceptionType;
  }

  public BusinessExceptionType getBusinessExceptionType() {
    return businessExceptionType;
  }
}

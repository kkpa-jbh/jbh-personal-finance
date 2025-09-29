package com.jbh.account.domain.exceptions;

// FIXME Decide to use this exception or not
public class GenericSpecificationException extends RuntimeException {

  private static final long serialVersionUID = 1L;

  public GenericSpecificationException(final String message) {
    super(message);
  }
}

package com.jbh.commons.exception;

// TODO: decide if this exception stays, or if callers should throw BusinessException instead.
public class GenericSpecificationException extends RuntimeException {

  private static final long serialVersionUID = 1L;

  public GenericSpecificationException(final String message) {
    super(message);
  }
}

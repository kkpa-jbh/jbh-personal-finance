package com.jbh.finance.application.acid;

import com.jbh.commons.exception.BusinessException;
import java.util.function.Supplier;

public interface UnitOfWork {

  void execute(Runnable action);

  <T> T executeWithResult(Supplier<T> action);

  /**
   * Runs the work in one transaction. A failure rolls the transaction back and the same exception
   * reaches the caller.
   */
  <T> T executeInTransaction(TransactionalWork<T> work) throws BusinessException;

  /** Work that may throw the checked {@link BusinessException}. */
  @FunctionalInterface
  interface TransactionalWork<T> {
    T run() throws BusinessException;
  }
}

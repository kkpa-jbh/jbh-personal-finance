package com.jbh.finance.test.testfixtures.fakes.acid;

import com.jbh.commons.exception.BusinessException;
import com.jbh.finance.application.acid.UnitOfWork;
import java.util.function.Supplier;

/**
 * Unit of work double for single-thread tests. It runs the work at once and records whether a
 * transaction is open, and how many were started, so a test can check where a call happens.
 */
public class RecordingUnitOfWork implements UnitOfWork {

  private boolean inTransaction;
  private int transactionCount;

  public boolean isInTransaction() {
    return inTransaction;
  }

  public int transactionCount() {
    return transactionCount;
  }

  @Override
  public void execute(final Runnable action) {
    executeWithResult(
        () -> {
          action.run();
          return null;
        });
  }

  @Override
  public <T> T executeWithResult(final Supplier<T> action) {
    final boolean wasInTransaction = inTransaction;
    inTransaction = true;
    transactionCount++;
    try {
      return action.get();
    } finally {
      inTransaction = wasInTransaction;
    }
  }

  @Override
  public <T> T executeInTransaction(final TransactionalWork<T> work) throws BusinessException {
    final boolean wasInTransaction = inTransaction;
    inTransaction = true;
    transactionCount++;
    try {
      return work.run();
    } finally {
      inTransaction = wasInTransaction;
    }
  }
}

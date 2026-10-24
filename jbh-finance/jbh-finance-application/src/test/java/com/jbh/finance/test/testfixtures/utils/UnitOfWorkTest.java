package com.jbh.finance.test.testfixtures.utils;

import com.jbh.commons.exception.BusinessException;
import com.jbh.finance.application.acid.UnitOfWork;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Supplier;

/**
 * Test unit of work. One shared reentrant lock stands in for the product row lock of the database:
 * a background task waits until the caller's unit of work ends.
 */
public class UnitOfWorkTest implements UnitOfWork {

  private static final ReentrantLock LOCK = new ReentrantLock();

  @Override
  public void execute(final Runnable action) {
    LOCK.lock();
    try {
      action.run();
    } finally {
      LOCK.unlock();
    }
  }

  @Override
  public <T> T executeWithResult(final Supplier<T> action) {
    LOCK.lock();
    try {
      return action.get();
    } finally {
      LOCK.unlock();
    }
  }

  @Override
  public <T> T executeInTransaction(final TransactionalWork<T> work) throws BusinessException {
    LOCK.lock();
    try {
      return work.run();
    } finally {
      LOCK.unlock();
    }
  }
}

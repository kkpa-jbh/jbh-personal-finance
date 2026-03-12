package com.jbh.finance.test.testfixtures.utils;

import com.jbh.finance.application.acid.UnitOfWork;
import java.util.function.Supplier;

public class UnitOfWorkTest implements UnitOfWork {

  @Override
  public void execute(final Runnable action) {
    action.run();
  }

  @Override
  public <T> T executeWithResult(final Supplier<T> action) {
    return action.get();
  }
}

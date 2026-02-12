package com.jbh.finance.application.acid;

import java.util.function.Supplier;

public interface UnitOfWork {

  void execute(Runnable action);

  <T> T executeWithResult(Supplier<T> action);
}

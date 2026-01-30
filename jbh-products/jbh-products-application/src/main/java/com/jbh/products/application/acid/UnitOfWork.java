package com.jbh.products.application.acid;

import java.util.function.Supplier;

public interface UnitOfWork {

  void execute(Runnable action);

  <T> T executeWithResult(Supplier<T> action);
}

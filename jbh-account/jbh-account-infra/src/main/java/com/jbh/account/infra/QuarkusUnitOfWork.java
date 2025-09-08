package com.jbh.account.infra;

import com.jbh.account.application.acid.UnitOfWork;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;
import java.util.function.Supplier;

@ApplicationScoped
@Transactional
public class QuarkusUnitOfWork implements UnitOfWork {


  @Override
  public void execute(Runnable action) {
    action.run();
  }

  @Override
  public <T> T executeWithResult(Supplier<T> action) {
    return action.get();
  }
}


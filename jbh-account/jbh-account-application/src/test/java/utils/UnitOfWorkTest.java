package utils;

import com.jbh.account_app.acid.UnitOfWork;
import java.util.function.Supplier;

public class UnitOfWorkTest implements UnitOfWork {

  @Override
  public void execute(Runnable action) {
    action.run();
  }

  @Override
  public <T> T executeWithResult(Supplier<T> action) {
    return action.get();
  }
}

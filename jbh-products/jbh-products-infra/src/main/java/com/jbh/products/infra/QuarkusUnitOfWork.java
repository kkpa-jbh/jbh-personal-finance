package com.jbh.products.infra;

import com.jbh.products.application.acid.UnitOfWork;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;
import java.util.function.Supplier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ApplicationScoped
@Transactional
public class QuarkusUnitOfWork implements UnitOfWork {

  private static final Logger LOG = LoggerFactory.getLogger(QuarkusUnitOfWork.class);

  @Override
  public void execute(final Runnable action) {
    LOG.info("Executing action in Unit of Work");
    try {
      action.run();
    } catch (final Exception e) {
      // FIXME: Identify which unit of work is being executed and retry it later
      LOG.error("Error executing action in Unit of Work", e);
    }
  }

  @Override
  public <T> T executeWithResult(final Supplier<T> action) {
    LOG.info("Executing action with result in Unit of Work");
    try {
      return action.get();
    } catch (final Exception e) {
      // FIXME: Identify which unit of work is being executed and retry it later
      LOG.error("Error executing action with result in Unit of Work", e);
      throw new IllegalStateException("Error executing action with result in Unit of Work", e);
    }
  }
}

package com.jbh.finance.infra;

import com.jbh.commons.exception.BusinessException;
import com.jbh.finance.application.acid.UnitOfWork;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;
import java.util.function.Supplier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ApplicationScoped
@Transactional(rollbackOn = Exception.class)
public class QuarkusUnitOfWork implements UnitOfWork {

  private static final Logger LOG = LoggerFactory.getLogger(QuarkusUnitOfWork.class);

  @Override
  public void execute(final Runnable action) {
    LOG.info("Executing action in Unit of Work");
    try {
      action.run();
    } catch (final RuntimeException e) {
      LOG.error("Error executing action in Unit of Work", e);
      throw e;
    }
  }

  @Override
  public <T> T executeWithResult(final Supplier<T> action) {
    LOG.info("Executing action with result in Unit of Work");
    try {
      return action.get();
    } catch (final RuntimeException e) {
      LOG.error("Error executing action with result in Unit of Work", e);
      throw new IllegalStateException("Error executing action with result in Unit of Work", e);
    }
  }

  @Override
  public <T> T executeInTransaction(final TransactionalWork<T> work) throws BusinessException {
    LOG.info("Executing transactional work in Unit of Work");
    try {
      return work.run();
    } catch (final BusinessException | RuntimeException e) {
      LOG.error("Error executing transactional work in Unit of Work", e);
      throw e;
    }
  }
}

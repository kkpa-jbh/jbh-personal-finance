package com.jbh.finance.infra.adapters.in.service;

import com.jbh.finance.application.feature.statement.commands.ExtractStatementMovementsCommand;
import jakarta.annotation.PreDestroy;
import jakarta.enterprise.context.ApplicationScoped;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Starts a statement import in the background, so the REST call answers at once and no HTTP
 * timeout can cut it. Each import runs on its own virtual thread.
 */
@ApplicationScoped
public class StatementImportDispatcher {

  private static final Logger LOG = LoggerFactory.getLogger(StatementImportDispatcher.class);
  private static final String LOG_PREFIX = "[StatementImport:Async]";

  private final StatementImportRunner runner;
  private final ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor();

  public StatementImportDispatcher(final StatementImportRunner runner) {
    this.runner = runner;
  }

  /**
   * Starts the import and returns at once.
   *
   * @param command the unlocked PDF
   */
  public void dispatch(final ExtractStatementMovementsCommand command) {
    executor.submit(() -> runner.run(command));
    LOG.info(LOG_PREFIX + " Started in the background");
  }

  @PreDestroy
  void stop() {
    executor.shutdown();
  }
}

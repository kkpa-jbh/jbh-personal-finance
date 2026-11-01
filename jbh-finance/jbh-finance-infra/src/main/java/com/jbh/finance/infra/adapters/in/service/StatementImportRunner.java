package com.jbh.finance.infra.adapters.in.service;

import com.jbh.finance.application.feature.statement.commands.RunStatementImportCommand;
import com.jbh.finance.application.feature.statement.usecases.RunStatementImportUseCase;
import com.jbh.finance.infra.adapters.out.ai.llmcompare.JevReport;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.context.control.ActivateRequestContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Runs one statement import on a background thread (see {@link StatementImportDispatcher}).
 *
 * <p>{@code @ActivateRequestContext}: a background thread has no CDI request context, and the
 * category read and the save (Hibernate) need one. It works only when called through the CDI
 * proxy, so this is a separate bean.
 *
 * <p>The run use case saves the movements and marks the import. {@link JevReport} writes the
 * classification to a text file when the LLM comparison is on (dev only).
 */
@ApplicationScoped
public class StatementImportRunner {

  private static final Logger LOG = LoggerFactory.getLogger(StatementImportRunner.class);
  private static final String LOG_PREFIX = "[StatementImport:Async]";

  private final RunStatementImportUseCase useCase;
  private final JevReport jevReport;

  public StatementImportRunner(
      final RunStatementImportUseCase useCase, final JevReport jevReport) {
    this.useCase = useCase;
    this.jevReport = jevReport;
  }

  /**
   * Runs the import. The use case never throws; it marks the import {@code FAILED} instead.
   *
   * @param command the import id, user, product and the unlocked PDF
   */
  @ActivateRequestContext
  public void run(final RunStatementImportCommand command) {
    useCase
        .run(command)
        .ifPresent(
            classification -> {
              LOG.info(LOG_PREFIX + " Finished: {} movements", classification.movements().size());
              jevReport.write(classification);
            });
  }
}

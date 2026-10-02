package com.jbh.finance.infra.adapters.in.service;

import com.jbh.commons.exception.BusinessException;
import com.jbh.finance.application.feature.statement.commands.ExtractStatementMovementsCommand;
import com.jbh.finance.application.feature.statement.dto.StatementClassificationDTO;
import com.jbh.finance.application.feature.statement.usecases.ExtractStatementMovementsUseCase;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.context.control.ActivateRequestContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Runs one statement import on a background thread (see {@link StatementImportDispatcher}).
 *
 * <p>{@code @ActivateRequestContext}: a background thread has no CDI request context, and the
 * category read (Hibernate) needs one. It works only when called through the CDI proxy, so this is
 * a separate bean.
 *
 * <p>The result is not returned or saved yet: only the movement count is logged.
 */
@ApplicationScoped
public class StatementImportRunner {

  private static final Logger LOG = LoggerFactory.getLogger(StatementImportRunner.class);
  private static final String LOG_PREFIX = "[StatementImport:Async]";

  private final ExtractStatementMovementsUseCase useCase;

  public StatementImportRunner(final ExtractStatementMovementsUseCase useCase) {
    this.useCase = useCase;
  }

  /**
   * Runs the import. Never throws: every failure is logged, or it would be lost.
   *
   * @param command the unlocked PDF
   */
  @ActivateRequestContext
  // A background task: any failure is logged here, or it would be lost.
  @SuppressWarnings("PMD.AvoidCatchingGenericException")
  public void run(final ExtractStatementMovementsCommand command) {
    try {
      final StatementClassificationDTO dto = useCase.extract(command);
      LOG.info(LOG_PREFIX + " Finished: {} movements", dto.movements().size());
    } catch (final BusinessException e) {
      LOG.warn(LOG_PREFIX + " Failed: {}", e.getBusinessExceptionType());
    } catch (final RuntimeException e) {
      // Only the class name: the message may carry statement data.
      LOG.error(LOG_PREFIX + " Failed: {}", e.getClass().getSimpleName());
    }
  }
}

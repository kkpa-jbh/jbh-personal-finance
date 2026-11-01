package com.jbh.finance.test.testfixtures.fakes.statement;

import com.jbh.commons.exception.BusinessException;
import com.jbh.commons.exception.BusinessExceptionType;
import com.jbh.finance.application.feature.statement.commands.ExtractStatementMovementsCommand;
import com.jbh.finance.application.feature.statement.dto.StatementClassificationDTO;
import com.jbh.finance.application.feature.statement.usecases.ExtractStatementMovementsUseCase;
import io.quarkus.test.Mock;
import jakarta.enterprise.context.ApplicationScoped;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Replaces the statement reader and classifier in every {@code @QuarkusTest}, so no test calls
 * Azure, OpenRouter or Jev. A test programs the next answers with {@link #returning} or {@link
 * #failingWith}. The answer applies to every PDF until the test changes it, so a test must wait
 * for its imports to finish before it ends.
 */
@Mock
@ApplicationScoped
public class FakeExtractStatementMovementsUseCase implements ExtractStatementMovementsUseCase {

  private final AtomicReference<Outcome> outcome = new AtomicReference<>(Outcome.NOT_PROGRAMMED);

  /** Makes every {@code extract} call answer with this classification. */
  public void returning(final StatementClassificationDTO classification) {
    outcome.set(new Outcome(classification, null));
  }

  /** Makes every {@code extract} call fail with this business error. */
  public void failingWith(final BusinessExceptionType failure) {
    outcome.set(new Outcome(null, failure));
  }

  @Override
  public StatementClassificationDTO extract(final ExtractStatementMovementsCommand command)
      throws BusinessException {
    final Outcome current = outcome.get();
    if (current.failure() != null) {
      throw new BusinessException(current.failure());
    }
    if (current.result() == null) {
      throw new IllegalStateException("The test did not program the fake statement reader");
    }
    return current.result();
  }

  private record Outcome(StatementClassificationDTO result, BusinessExceptionType failure) {
    static final Outcome NOT_PROGRAMMED = new Outcome(null, null);
  }
}

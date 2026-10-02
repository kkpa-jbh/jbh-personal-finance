package com.jbh.finance.infra.adapters.out.ai.llmcompare;

import com.jbh.commons.exception.BusinessException;
import com.jbh.finance.application.feature.statement.dto.ExtractedStatementDTO;
import com.jbh.finance.infra.adapters.out.ai.llm.StatementExtractionFormat;
import com.jbh.finance.infra.adapters.out.ai.llm.StatementLlm;
import java.time.Duration;

/**
 * Wraps the active {@link StatementLlm}. It returns the active result unchanged, then starts the
 * comparison in the background with the same text and the active model's answer.
 *
 * <p>Not a CDI bean on purpose: as a bean it would be one more {@link StatementLlm} to compare.
 */
class ComparingStatementLlm implements StatementLlm {

  private final StatementLlm active;
  private final String activeName;
  private final LlmComparison comparison;

  ComparingStatementLlm(
      final StatementLlm active, final String activeName, final LlmComparison comparison) {
    this.active = active;
    this.activeName = activeName;
    this.comparison = comparison;
  }

  @Override
  public String answer(final String statementText) throws BusinessException {
    return active.answer(statementText);
  }

  @Override
  public ExtractedStatementDTO extract(final String statementText) throws BusinessException {
    final long started = System.nanoTime();
    final String answer;
    try {
      answer = active.answer(statementText);
    } catch (final BusinessException e) {
      comparison.startInBackground(
          statementText,
          ProviderRun.failed(
              activeName, String.valueOf(e.getBusinessExceptionType()), since(started)));
      throw e;
    }
    comparison.startInBackground(
        statementText, ProviderRun.answered(activeName, answer, since(started)));
    return StatementExtractionFormat.parse(answer);
  }

  private static Duration since(final long started) {
    return Duration.ofNanos(System.nanoTime() - started);
  }
}

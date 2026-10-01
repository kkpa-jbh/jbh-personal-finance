package com.jbh.finance.infra.adapters.out.ai;

import com.jbh.commons.exception.BusinessException;
import com.jbh.finance.application.feature.statement.dto.ExtractedStatementDTO;

/**
 * Step 2 of the statement reader: a language model finds the movements and balances in the text.
 *
 * <p>An infra seam, not an application port. A new LLM provider is one new class plus one {@code
 * StatementImportConfig.Llm} value. Every implementation uses {@link StatementExtractionFormat} for
 * the prompt rules and the parsing, so all providers answer the same way.
 */
public interface StatementLlm {

  /**
   * Extracts the movements and balances of a statement.
   *
   * @param statementText the text from a {@link StatementTextSource}
   * @return the extracted statement
   * @throws BusinessException when the provider is not configured or fails
   */
  ExtractedStatementDTO extract(String statementText) throws BusinessException;
}

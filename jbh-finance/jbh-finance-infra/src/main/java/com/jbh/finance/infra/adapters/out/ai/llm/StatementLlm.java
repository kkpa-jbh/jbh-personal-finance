package com.jbh.finance.infra.adapters.out.ai.llm;

import com.jbh.commons.exception.BusinessException;
import com.jbh.finance.application.feature.statement.dto.ExtractedStatementDTO;
import com.jbh.finance.infra.adapters.out.ai.StatementTextSource;

/**
 * Step 2 of the statement reader: a language model finds the movements and balances in the text.
 *
 * <p>An infra seam, not an application port. A new LLM provider is one new class plus one {@code
 * StatementImportConfig.Llm} value. Every implementation uses {@link StatementExtractionFormat} for
 * the prompt rules and the parsing, so all providers answer the same way.
 *
 * <p>A provider implements only {@link #answer}; {@link #extract} parses that answer.
 */
public interface StatementLlm {

  /**
   * Sends the statement text to the model and returns its answer as it came.
   *
   * @param statementText the text from a {@link StatementTextSource}
   * @return the model's answer: the JSON statement, as text
   * @throws BusinessException when the provider is not configured, fails, or returns no answer
   */
  String answer(String statementText) throws BusinessException;

  /**
   * Extracts the movements and balances of a statement.
   *
   * @param statementText the text from a {@link StatementTextSource}
   * @return the extracted statement
   * @throws BusinessException when the provider is not configured or fails, or the answer is not
   *     valid JSON
   */
  default ExtractedStatementDTO extract(final String statementText) throws BusinessException {
    return StatementExtractionFormat.parse(answer(statementText));
  }

  /**
   * The request body this provider sends for the text, as readable JSON. Only for the LLM
   * comparison files (temporary). It never holds the API key: keys travel in headers.
   *
   * @param statementText the text from a {@link StatementTextSource}
   * @return the JSON body, or an empty text when the provider does not show it
   */
  default String requestBody(final String statementText) {
    return "";
  }
}

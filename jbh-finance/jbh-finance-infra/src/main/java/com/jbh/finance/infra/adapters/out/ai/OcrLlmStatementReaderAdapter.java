package com.jbh.finance.infra.adapters.out.ai;

import com.jbh.commons.exception.BusinessException;
import com.jbh.finance.application.feature.statement.dto.ExtractedStatementDTO;
import com.jbh.finance.application.feature.statement.ports.output.StatementReader;

/**
 * The {@link StatementReader} of the application: step 1 turns the PDF into text, step 2 an LLM
 * finds the movements and balances in that text.
 *
 * <p>Both steps are chosen by config ({@code jbh.statement.text-source}, {@code
 * jbh.statement.llm}) in {@code StatementUseCasesCDIConfig}. Not a CDI bean itself: the config
 * class builds it.
 */
public class OcrLlmStatementReaderAdapter implements StatementReader {

  private final StatementTextSource textSource;
  private final StatementLlm llm;

  public OcrLlmStatementReaderAdapter(final StatementTextSource textSource, final StatementLlm llm) {
    this.textSource = textSource;
    this.llm = llm;
  }

  @Override
  public ExtractedStatementDTO read(final byte[] pdfContent) throws BusinessException {
    return llm.extract(textSource.extractText(pdfContent));
  }
}

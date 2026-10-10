package com.jbh.finance.infra.adapters.out.ai;

import com.jbh.commons.exception.BusinessException;
import com.jbh.finance.application.feature.statement.dto.ExtractedStatementDTO;
import com.jbh.finance.application.feature.statement.ports.output.StatementReader;
import com.jbh.finance.infra.adapters.out.ai.llm.StatementLlm;
import java.time.Duration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The {@link StatementReader} of the application: step 1 turns the PDF into text, step 2 an LLM
 * finds the movements and balances in that text.
 *
 * <p>Both steps are chosen by config ({@code jbh.statement.text-source}, {@code
 * jbh.statement.llm}) in {@code StatementUseCasesCDIConfig}. Not a CDI bean itself: the config
 * class builds it.
 */
public class OcrLlmStatementReaderAdapter implements StatementReader {

  private static final Logger LOGGER =
      LoggerFactory.getLogger(OcrLlmStatementReaderAdapter.class);
  private static final String LOG_PREFIX = "[StatementImport:Reader]";

  private final StatementTextSource textSource;
  private final StatementLlm llm;

  public OcrLlmStatementReaderAdapter(final StatementTextSource textSource, final StatementLlm llm) {
    this.textSource = textSource;
    this.llm = llm;
  }

  @Override
  public ExtractedStatementDTO read(final byte[] pdfContent) throws BusinessException {
    LOGGER.info(LOG_PREFIX + " Step 1a PDF to text started");
    long started = System.nanoTime();
    final String text = textSource.extractText(pdfContent);
    LOGGER.info(
        LOG_PREFIX + " Step 1a PDF to text done in {} ms: {} characters",
        Duration.ofNanos(System.nanoTime() - started).toMillis(),
        text.length());

    LOGGER.info(LOG_PREFIX + " Step 1b LLM extraction started");
    started = System.nanoTime();
    final ExtractedStatementDTO statement = llm.extract(text);
    LOGGER.info(
        LOG_PREFIX + " Step 1b LLM extraction done in {} ms",
        Duration.ofNanos(System.nanoTime() - started).toMillis());
    return statement;
  }
}

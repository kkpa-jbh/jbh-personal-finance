package com.jbh.finance.infra.adapters.out.ai;

import com.jbh.commons.exception.BusinessException;

/**
 * Step 1 of the statement reader: turns an unlocked PDF into text (plain text or markdown).
 *
 * <p>An infra seam, not an application port: the use case only knows {@code StatementReader}. A new
 * source is one new class plus one {@code StatementImportConfig.TextSource} value.
 */
public interface StatementTextSource {

  /**
   * Returns the text of the whole statement, page after page.
   *
   * @param pdfContent the unlocked PDF bytes
   * @return the text, never blank
   * @throws BusinessException when the PDF cannot be read or the provider fails
   */
  String extractText(byte[] pdfContent) throws BusinessException;
}

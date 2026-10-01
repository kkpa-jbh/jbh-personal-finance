package com.jbh.finance.infra.adapters.out.ai;

import com.jbh.commons.exception.BusinessException;
import com.jbh.finance.infra.adapters.exceptions.FinanceInfraBusinessExceptionType;
import jakarta.enterprise.context.ApplicationScoped;
import java.io.IOException;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;

/**
 * Extracts the text of an unlocked statement PDF locally, with PDFBox. Nothing leaves the server
 * in this step. Active when {@code jbh.statement.text-source=pdfbox}.
 *
 * <p>No OCR: a scanned PDF has no text and fails with {@code STATEMENT_PDF_HAS_NO_TEXT}. The text
 * is personal data: it is never logged.
 */
@ApplicationScoped
public class PdfBoxTextSource implements StatementTextSource {

  /**
   * Returns the text of the PDF, page after page, with each row in reading order.
   *
   * @param pdfContent the unlocked PDF bytes
   * @return the text, never blank
   * @throws BusinessException when the PDF cannot be opened, or has no text (a scanned PDF)
   */
  // The cause is dropped on purpose: PDFBox messages may carry details of the document.
  @SuppressWarnings("PMD.PreserveStackTrace")
  @Override
  public String extractText(final byte[] pdfContent) throws BusinessException {
    final String text;
    try (PDDocument document = Loader.loadPDF(pdfContent)) {
      final PDFTextStripper stripper = new PDFTextStripper();
      // Bank statements are tables: sort by position so each row stays on one line.
      stripper.setSortByPosition(true);
      text = stripper.getText(document);
    } catch (final IOException e) {
      throw new BusinessException(FinanceInfraBusinessExceptionType.STATEMENT_UNREADABLE_PDF);
    }

    if (text == null || text.isBlank()) {
      throw new BusinessException(FinanceInfraBusinessExceptionType.STATEMENT_PDF_HAS_NO_TEXT);
    }
    return text;
  }
}

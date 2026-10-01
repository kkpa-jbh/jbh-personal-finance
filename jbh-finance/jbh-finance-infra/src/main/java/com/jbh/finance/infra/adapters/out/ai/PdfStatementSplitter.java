package com.jbh.finance.infra.adapters.out.ai;

import com.jbh.commons.exception.BusinessException;
import com.jbh.finance.infra.adapters.exceptions.FinanceInfraBusinessExceptionType;
import jakarta.enterprise.context.ApplicationScoped;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.multipdf.Splitter;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Cuts an unlocked PDF into parts of a fixed number of pages, in memory, with PDFBox.
 *
 * <p>For providers with a page limit per request, for example the Azure free tier (2 pages). Any
 * provider can use it.
 */
@ApplicationScoped
public class PdfStatementSplitter {

  private static final Logger LOGGER = LoggerFactory.getLogger(PdfStatementSplitter.class);
  private static final String LOG_PREFIX = "[StatementImport:PdfBoxSplit]";
  private static final int NO_SPLIT = 0;

  /**
   * Splits the PDF.
   *
   * @param pdfContent the unlocked PDF bytes
   * @param pagesPerPart pages in each part; zero or less returns the PDF as one part
   * @return the parts, in page order
   * @throws BusinessException when the PDF cannot be opened
   */
  // The cause is dropped on purpose: PDFBox messages may carry details of the document.
  @SuppressWarnings("PMD.PreserveStackTrace")
  public List<byte[]> split(final byte[] pdfContent, final int pagesPerPart)
      throws BusinessException {
    if (pagesPerPart <= NO_SPLIT) {
      return List.of(pdfContent);
    }

    try (PDDocument document = Loader.loadPDF(pdfContent)) {
      if (document.getNumberOfPages() <= pagesPerPart) {
        LOGGER.info(LOG_PREFIX + " No split needed: {} pages", document.getNumberOfPages());
        return List.of(pdfContent);
      }

      final Splitter splitter = new Splitter();
      splitter.setSplitAtPage(pagesPerPart);
      final List<byte[]> parts = new ArrayList<>();
      for (final PDDocument part : splitter.split(document)) {
        try (part) {
          final ByteArrayOutputStream output = new ByteArrayOutputStream();
          part.save(output);
          parts.add(output.toByteArray());
        }
      }
      LOGGER.info(
          LOG_PREFIX + " Split {} pages into {} parts of up to {} pages",
          document.getNumberOfPages(),
          parts.size(),
          pagesPerPart);
      return parts;

    } catch (final IOException e) {
      throw new BusinessException(FinanceInfraBusinessExceptionType.STATEMENT_UNREADABLE_PDF);
    }
  }
}

package com.jbh.finance.infra.adapters.in.service;

import com.jbh.commons.exception.BusinessException;
import com.jbh.finance.infra.adapters.exceptions.FinanceInfraBusinessExceptionType;
import jakarta.enterprise.context.ApplicationScoped;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.encryption.InvalidPasswordException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Removes the password of a bank statement PDF in memory, so an AI provider can read it.
 *
 * <p>The password is personal data: it is never logged and never put in an exception.
 */
@ApplicationScoped
public class PdfStatementUnlocker {

  private static final Logger LOGGER = LoggerFactory.getLogger(PdfStatementUnlocker.class);
  private static final String NO_PASSWORD = "";

  /**
   * Returns the PDF without encryption.
   *
   * @param pdfContent the uploaded PDF bytes
   * @param password the PDF password, or {@code null} when the PDF is not locked
   * @return the unlocked PDF bytes; the same bytes when the PDF is not encrypted
   * @throws BusinessException when the password is missing or wrong, or the PDF cannot be opened
   */
  // The cause is dropped on purpose: PDFBox messages may carry details of the document.
  @SuppressWarnings("PMD.PreserveStackTrace")
  public byte[] unlock(final byte[] pdfContent, final String password) throws BusinessException {
    final boolean noPassword = password == null || password.isBlank();

    try (PDDocument document =
        Loader.loadPDF(pdfContent, password == null ? NO_PASSWORD : password)) {
      if (!document.isEncrypted()) {
        LOGGER.debug("Statement PDF is not encrypted");
        return pdfContent;
      }

      // A PDF with only an owner password opens with "" and is still encrypted.
      document.setAllSecurityToBeRemoved(true);
      final ByteArrayOutputStream output = new ByteArrayOutputStream();
      document.save(output);
      LOGGER.debug("Statement PDF unlocked");
      return output.toByteArray();

    } catch (final InvalidPasswordException e) {
      throw new BusinessException(
          noPassword
              ? FinanceInfraBusinessExceptionType.STATEMENT_PASSWORD_REQUIRED
              : FinanceInfraBusinessExceptionType.STATEMENT_WRONG_PASSWORD);
    } catch (final IOException e) {
      throw new BusinessException(FinanceInfraBusinessExceptionType.STATEMENT_UNREADABLE_PDF);
    }
  }
}

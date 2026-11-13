package com.jbh.finance.infra.adapters.in.service;

import com.jbh.commons.exception.BusinessException;
import com.jbh.finance.infra.adapters.exceptions.FinanceInfraBusinessExceptionType;
import com.jbh.finance.infra.config.StatementImportConfig;
import jakarta.enterprise.context.ApplicationScoped;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Locale;
import org.jboss.resteasy.reactive.multipart.FileUpload;

/**
 * Checks an uploaded statement (present, a PDF, not too large) and reads its bytes. The bytes are
 * personal data: nothing here logs them.
 */
@ApplicationScoped
public class StatementUploadReader {

  private static final String PDF_EXTENSION = ".pdf";
  private static final String PDF_MIME_TYPE = "application/pdf";

  private final StatementImportConfig config;

  public StatementUploadReader(final StatementImportConfig config) {
    this.config = config;
  }

  /**
   * Validates the upload and reads the original bytes.
   *
   * @param fileUpload the uploaded file, may be {@code null}
   * @return the bytes exactly as the client sent them
   * @throws BusinessException {@code STATEMENT_FILE_REQUIRED}, {@code STATEMENT_FILE_NOT_PDF},
   *     {@code STATEMENT_FILE_TOO_LARGE} or {@code STATEMENT_UNREADABLE_PDF}
   */
  // The cause is dropped on purpose: the upload is personal data.
  @SuppressWarnings("PMD.PreserveStackTrace")
  public byte[] read(final FileUpload fileUpload) throws BusinessException {
    if (fileUpload == null) {
      throw new BusinessException(FinanceInfraBusinessExceptionType.STATEMENT_FILE_REQUIRED);
    }
    if (!isPdf(fileUpload)) {
      throw new BusinessException(FinanceInfraBusinessExceptionType.STATEMENT_FILE_NOT_PDF);
    }
    if (fileUpload.size() > config.maxFileSizeBytes()) {
      throw new BusinessException(FinanceInfraBusinessExceptionType.STATEMENT_FILE_TOO_LARGE);
    }
    try {
      return Files.readAllBytes(fileUpload.uploadedFile());
    } catch (final IOException e) {
      throw new BusinessException(FinanceInfraBusinessExceptionType.STATEMENT_UNREADABLE_PDF);
    }
  }

  private static boolean isPdf(final FileUpload fileUpload) {
    final String fileName = fileUpload.fileName();
    final boolean pdfName =
        fileName != null && fileName.toLowerCase(Locale.ROOT).endsWith(PDF_EXTENSION);
    return pdfName || PDF_MIME_TYPE.equalsIgnoreCase(fileUpload.contentType());
  }
}

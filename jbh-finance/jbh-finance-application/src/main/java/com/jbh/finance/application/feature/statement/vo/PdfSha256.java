package com.jbh.finance.application.feature.statement.vo;

/**
 * The SHA-256 of the bytes of an uploaded PDF, as 64 lower-case hex characters. It tells whether
 * the same statement was already imported; the PDF itself is never stored.
 *
 * @param value lower-case hex digest
 */
public record PdfSha256(String value) {

  /**
   * Computes the digest of PDF bytes.
   *
   * @param pdfContent the bytes, not {@code null}
   * @return the digest
   * @throws IllegalArgumentException when the bytes are {@code null}
   */
  public static PdfSha256 of(final byte[] pdfContent) {
    throw new UnsupportedOperationException("not implemented");
  }
}

package com.jbh.finance.application.feature.statement.vo;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

/**
 * The SHA-256 of the bytes of an uploaded PDF, as 64 lower-case hex characters. It tells whether
 * the same statement was already imported; the PDF itself is never stored.
 *
 * @param value lower-case hex digest
 */
public record PdfSha256(String value) {

  private static final String ALGORITHM = "SHA-256";

  /**
   * Computes the digest of PDF bytes.
   *
   * @param pdfContent the bytes, not {@code null}
   * @return the digest
   * @throws IllegalArgumentException when the bytes are {@code null}
   */
  public static PdfSha256 of(final byte[] pdfContent) {
    if (pdfContent == null) {
      throw new IllegalArgumentException("PDF content cannot be null");
    }
    try {
      final byte[] digest = MessageDigest.getInstance(ALGORITHM).digest(pdfContent);
      return new PdfSha256(HexFormat.of().formatHex(digest));
    } catch (final NoSuchAlgorithmException e) {
      throw new IllegalStateException(ALGORITHM + " is not available", e);
    }
  }
}

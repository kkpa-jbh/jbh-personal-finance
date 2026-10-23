package com.jbh.finance.application.feature.statement.commands;

import com.jbh.commons.exception.GenericSpecificationException;
import com.jbh.finance.application.shared.validation.CommandValidator;

/**
 * Asks to read and classify the movements of an unlocked statement PDF.
 *
 * <p>The PDF bytes are personal data: {@link #toString()} prints only their size. A {@code null}
 * PDF is kept as an empty array, so {@link #validate()} rejects it.
 *
 * @param pdfContent the unlocked PDF bytes
 */
public record ExtractStatementMovementsCommand(byte[] pdfContent) implements CommandValidator {

  private static final int EMPTY_LENGTH = 0;

  public ExtractStatementMovementsCommand(final byte[] pdfContent) {
    this.pdfContent = pdfContent == null ? new byte[EMPTY_LENGTH] : pdfContent.clone();
  }

  @Override
  public byte[] pdfContent() {
    return pdfContent.clone();
  }

  @Override
  public void validate() {
    if (pdfContent.length == EMPTY_LENGTH) {
      throw new GenericSpecificationException("PDF content cannot be empty");
    }
  }

  @Override
  public String toString() {
    return "ExtractStatementMovementsCommand[pdfContent=" + pdfContent.length + " bytes]";
  }
}

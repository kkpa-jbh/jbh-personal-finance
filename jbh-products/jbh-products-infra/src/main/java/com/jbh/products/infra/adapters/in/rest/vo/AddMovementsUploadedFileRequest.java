package com.jbh.products.infra.adapters.in.rest.vo;

import com.jbh.products.application.core.vo.commands.AddMovementUploadedFileCommand;
import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Value object for adding a movement with date and amount.
 *
 * @param entryDate Date of the movement
 * @param totalAmount Positive for deposit, negative for withdrawal
 * @param balanceSnapshot Current balance after the movement (optional)
 */
public record AddMovementsUploadedFileRequest(
    LocalDate entryDate, BigDecimal totalAmount, BigDecimal balanceSnapshot) {

  public void validate() {
    if (entryDate == null) {
      throw new IllegalArgumentException("Entry date cannot be null");
    }
    if (totalAmount == null && balanceSnapshot == null) {
      throw new IllegalArgumentException("There is not any amount to add");
    }
  }

  public AddMovementUploadedFileCommand toCommand() {
    return new AddMovementUploadedFileCommand(entryDate, totalAmount, balanceSnapshot);
  }
}

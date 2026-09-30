package com.jbh.finance.test.application.feature.statement.commands;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.jbh.commons.exception.GenericSpecificationException;
import com.jbh.finance.application.feature.statement.commands.ExtractStatementMovementsCommand;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

@Tag("unit")
class ExtractStatementMovementsCommandTest {

  private static final byte[] PDF_BYTES = {37, 80, 68, 70};

  @Test
  void shouldThrowWhenContentIsNull() {
    // Arrange
    final ExtractStatementMovementsCommand command = new ExtractStatementMovementsCommand(null);

    // Act & Assert
    assertThrows(GenericSpecificationException.class, command::validate);
  }

  @Test
  void shouldThrowWhenContentIsEmpty() {
    // Arrange
    final ExtractStatementMovementsCommand command =
        new ExtractStatementMovementsCommand(new byte[0]);

    // Act & Assert
    assertThrows(GenericSpecificationException.class, command::validate);
  }

  @Test
  void shouldPassValidationWhenContentIsNotEmpty() {
    // Arrange
    final ExtractStatementMovementsCommand command =
        new ExtractStatementMovementsCommand(PDF_BYTES.clone());

    // Act & Assert
    assertDoesNotThrow(command::validate);
  }

  @Test
  void shouldKeepContentWhenSourceArrayChanges() {
    // Arrange
    final byte[] source = PDF_BYTES.clone();
    final ExtractStatementMovementsCommand command = new ExtractStatementMovementsCommand(source);

    // Act
    source[0] = 0;
    command.pdfContent()[1] = 0;

    // Assert
    assertArrayEquals(PDF_BYTES, command.pdfContent());
  }

  @Test
  void shouldHideBytesWhenPrinted() {
    // Arrange
    final ExtractStatementMovementsCommand command =
        new ExtractStatementMovementsCommand(PDF_BYTES.clone());

    // Act
    final String text = command.toString();

    // Assert
    assertTrue(text.contains("4 bytes"));
    assertFalse(text.contains("37"));
  }
}

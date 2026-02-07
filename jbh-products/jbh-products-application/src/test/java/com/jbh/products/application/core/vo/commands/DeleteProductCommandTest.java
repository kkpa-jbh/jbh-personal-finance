package com.jbh.products.application.core.vo.commands;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.jbh.commons.exception.GenericSpecificationException;
import com.jbh.products.domain.product.vo.ProductId;
import java.util.UUID;
import org.junit.jupiter.api.Test;

public class DeleteProductCommandTest {

  private static final UUID VALID_USER_ID = UUID.randomUUID();
  private static final ProductId VALID_PRODUCT_ID = ProductId.generate();

  @Test
  public void shouldCreateCommandWithValidParameters() {
    final DeleteProductCommand command =
        assertDoesNotThrow(() -> new DeleteProductCommand(VALID_USER_ID, VALID_PRODUCT_ID));

    assertNotNull(command);
    assertEquals(VALID_USER_ID, command.userId());
    assertEquals(VALID_PRODUCT_ID, command.productId());
  }

  @Test
  public void shouldThrowWhenUserIdIsNull() {
    final GenericSpecificationException exception =
        assertThrows(
            GenericSpecificationException.class,
            () -> new DeleteProductCommand(null, VALID_PRODUCT_ID));
    assertEquals("User ID cannot be null", exception.getMessage());
  }

  @Test
  public void shouldThrowWhenProductIdIsNull() {
    final GenericSpecificationException exception =
        assertThrows(
            GenericSpecificationException.class,
            () -> new DeleteProductCommand(VALID_USER_ID, null));
    assertEquals("Product ID cannot be null", exception.getMessage());
  }

  @Test
  public void shouldThrowWhenBothParametersAreNull() {
    assertThrows(GenericSpecificationException.class, () -> new DeleteProductCommand(null, null));
  }

  @Test
  public void shouldValidateSuccessfully() {
    final DeleteProductCommand command = new DeleteProductCommand(VALID_USER_ID, VALID_PRODUCT_ID);
    assertDoesNotThrow(command::validate);
  }
}

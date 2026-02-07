package com.jbh.products.application.core.vo.commands;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.jbh.commons.exception.GenericSpecificationException;
import com.jbh.products.application.feature.product.commands.EditProductCommand;
import com.jbh.products.domain.product.vo.ProductId;
import com.jbh.products.domain.product.vo.ProductMetadata;
import com.jbh.products.domain.product.vo.ProductMetadataKey;
import java.math.BigDecimal;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;

public class EditProductCommandTest {

  private static final UUID VALID_USER_ID = UUID.randomUUID();
  private static final ProductId VALID_PRODUCT_ID = ProductId.generate();
  private static final String VALID_NAME = "Test Product";
  private static final ProductMetadata VALID_METADATA =
      ProductMetadata.fromMap(Map.of(ProductMetadataKey.COMMON_INITIAL_BALANCE, BigDecimal.ZERO));

  @Test
  public void shouldCreateCommandWithNameOnly() {
    assertDoesNotThrow(
        () -> new EditProductCommand(VALID_USER_ID, VALID_PRODUCT_ID, VALID_NAME, null));
  }

  @Test
  public void shouldCreateCommandWithMetadataOnly() {
    assertDoesNotThrow(
        () -> new EditProductCommand(VALID_USER_ID, VALID_PRODUCT_ID, null, VALID_METADATA));
  }

  @Test
  public void shouldCreateCommandWithBothNameAndMetadata() {
    assertDoesNotThrow(
        () -> new EditProductCommand(VALID_USER_ID, VALID_PRODUCT_ID, VALID_NAME, VALID_METADATA));
  }

  @Test
  public void shouldThrowWhenUserIdIsNull() {
    final GenericSpecificationException exception =
        assertThrows(
            GenericSpecificationException.class,
            () -> new EditProductCommand(null, VALID_PRODUCT_ID, VALID_NAME, null));
    assertEquals("User ID cannot be null", exception.getMessage());
  }

  @Test
  public void shouldThrowWhenProductIdIsNull() {
    final GenericSpecificationException exception =
        assertThrows(
            GenericSpecificationException.class,
            () -> new EditProductCommand(VALID_USER_ID, null, VALID_NAME, null));
    assertEquals("Product ID cannot be null", exception.getMessage());
  }

  @Test
  public void shouldThrowWhenBothNameAndMetadataAreNull() {
    final GenericSpecificationException exception =
        assertThrows(
            GenericSpecificationException.class,
            () -> new EditProductCommand(VALID_USER_ID, VALID_PRODUCT_ID, null, null));
    assertEquals(
        "At least one of name or metadata must be provided for edit", exception.getMessage());
  }

  @Test
  public void shouldThrowWhenNameIsBlankAndMetadataIsNull() {
    final GenericSpecificationException exception =
        assertThrows(
            GenericSpecificationException.class,
            () -> new EditProductCommand(VALID_USER_ID, VALID_PRODUCT_ID, "   ", null));
    assertEquals(
        "At least one of name or metadata must be provided for edit", exception.getMessage());
  }

  @Test
  public void shouldThrowWhenNameIsEmptyAndMetadataIsNull() {
    final GenericSpecificationException exception =
        assertThrows(
            GenericSpecificationException.class,
            () -> new EditProductCommand(VALID_USER_ID, VALID_PRODUCT_ID, "", null));
    assertEquals(
        "At least one of name or metadata must be provided for edit", exception.getMessage());
  }

  @Test
  public void shouldThrowWhenNameIsNullAndMetadataIsEmpty() {
    final GenericSpecificationException exception =
        assertThrows(
            GenericSpecificationException.class,
            () ->
                new EditProductCommand(
                    VALID_USER_ID, VALID_PRODUCT_ID, null, ProductMetadata.empty()));
    assertEquals(
        "At least one of name or metadata must be provided for edit", exception.getMessage());
  }

  @Test
  public void shouldValidateSuccessfully() {
    final EditProductCommand command =
        new EditProductCommand(VALID_USER_ID, VALID_PRODUCT_ID, VALID_NAME, null);
    assertDoesNotThrow(command::validate);
  }
}

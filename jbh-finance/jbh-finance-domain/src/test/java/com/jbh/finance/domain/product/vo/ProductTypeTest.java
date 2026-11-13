package com.jbh.finance.domain.product.vo;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

@Tag("unit")
class ProductTypeTest {

  @Test
  void shouldAllowStatementImportWhenProductIsSavingsOrCreditCard() {
    for (final ProductType type : List.of(ProductType.SAVINGS, ProductType.CREDIT_CARD)) {
      assertTrue(type.allowsStatementImport(), type + " must allow statement import");
    }
  }

  @Test
  void shouldNotAllowStatementImportWhenProductIsOtherType() {
    final List<ProductType> others =
        List.of(
            ProductType.REAL_ESTATE_INVESTMENT,
            ProductType.LOAN,
            ProductType.INVESTMENT,
            ProductType.CDT);
    for (final ProductType type : others) {
      assertFalse(type.allowsStatementImport(), type + " must not allow statement import");
    }
  }
}

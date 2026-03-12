package com.jbh.finance.test.application.feature.product.ports.input;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.jbh.finance.application.feature.product.dto.MetadataFieldConfigDTO;
import com.jbh.finance.application.feature.product.ports.input.GetProductMetadataConfigInputPort;
import com.jbh.finance.application.feature.product.services.metadata.ProductMetadataConfigRegistry;
import com.jbh.finance.domain.product.vo.ProductType;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class GetProductMetadataConfigInputPortTest {

  private GetProductMetadataConfigInputPort inputPort;

  @BeforeEach
  void setUp() {
    final ProductMetadataConfigRegistry registry = new ProductMetadataConfigRegistry();
    inputPort = new GetProductMetadataConfigInputPort(registry);
  }

  @Test
  void shouldReturnConfigurationForValidProductType() {
    final List<MetadataFieldConfigDTO> result = inputPort.execute(ProductType.LOAN);

    assertNotNull(result);
    assertFalse(result.isEmpty());
  }

  @Test
  void shouldThrowExceptionWhenProductTypeIsNull() {
    assertThrows(IllegalArgumentException.class, () -> inputPort.execute(null));
  }

  @Test
  void shouldReturnConfigurationForAllProductTypes() {
    for (final ProductType productType : ProductType.values()) {
      final List<MetadataFieldConfigDTO> result = inputPort.execute(productType);
      assertNotNull(result);
    }
  }
}

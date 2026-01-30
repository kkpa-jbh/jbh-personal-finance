package com.jbh.products.application.core.ports.input;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.jbh.products.application.core.dto.MetadataFieldConfigDTO;
import com.jbh.products.application.core.services.metadata.ProductMetadataConfigRegistry;
import com.jbh.products.domain.vo.ProductType;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class GetProductMetadataConfigInputPortTest {

  private GetProductMetadataConfigInputPort inputPort;

  @BeforeEach
  void setUp() {
    ProductMetadataConfigRegistry registry = new ProductMetadataConfigRegistry();
    inputPort = new GetProductMetadataConfigInputPort(registry);
  }

  @Test
  void shouldReturnConfigurationForValidProductType() {
    List<MetadataFieldConfigDTO> result = inputPort.execute(ProductType.LOAN);

    assertNotNull(result);
    assertFalse(result.isEmpty());
  }

  @Test
  void shouldThrowExceptionWhenProductTypeIsNull() {
    assertThrows(IllegalArgumentException.class, () -> inputPort.execute(null));
  }

  @Test
  void shouldReturnConfigurationForAllProductTypes() {
    for (ProductType productType : ProductType.values()) {
      List<MetadataFieldConfigDTO> result = inputPort.execute(productType);
      assertNotNull(result);
    }
  }
}

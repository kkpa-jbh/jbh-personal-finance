package com.jbh.account.application.core.services.metadata;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.jbh.account.application.core.dto.MetadataFieldConfigDTO;
import com.jbh.account.domain.vo.ProductType;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class ProductMetadataConfigRegistryTest {

  private ProductMetadataConfigRegistry registry;

  @BeforeEach
  void setUp() {
    registry = new ProductMetadataConfigRegistry();
  }

  @Test
  void shouldReturnLoanConfigurationWithRequiredFieldsFirst() {
    final List<MetadataFieldConfigDTO> config = registry.getConfigurationFor(ProductType.LOAN);

    assertNotNull(config);
    assertFalse(config.isEmpty());

    final List<MetadataFieldConfigDTO> requiredFields =
        config.stream().filter(MetadataFieldConfigDTO::required).toList();
    assertFalse(requiredFields.isEmpty());
    assertTrue(requiredFields.stream().anyMatch(f -> f.key().equals("LOAN_PRINCIPAL_AMOUNT")));
    assertTrue(requiredFields.stream().anyMatch(f -> f.key().equals("LOAN_INTEREST_RATE")));
  }

  @Test
  void shouldReturnCreditCardConfiguration() {
    final List<MetadataFieldConfigDTO> config =
        registry.getConfigurationFor(ProductType.CREDIT_CARD);

    assertNotNull(config);
    assertTrue(config.stream().anyMatch(f -> f.key().equals("CREDIT_LIMIT")));
    assertTrue(config.stream().anyMatch(f -> f.key().equals("PAYMENT_DUE_DAY")));
  }

  @Test
  void shouldReturnInvestmentConfiguration() {
    final List<MetadataFieldConfigDTO> config =
        registry.getConfigurationFor(ProductType.INVESTMENT);

    assertNotNull(config);
    assertTrue(config.stream().anyMatch(f -> f.key().equals("BROKER_NAME")));
    assertTrue(config.stream().anyMatch(f -> f.key().equals("COMMISSION_RATE")));
  }

  @Test
  void shouldReturnCDTConfiguration() {
    final List<MetadataFieldConfigDTO> config = registry.getConfigurationFor(ProductType.CDT);

    assertNotNull(config);
    assertTrue(config.stream().anyMatch(f -> f.key().equals("MATURITY_DATE")));
    assertTrue(config.stream().anyMatch(f -> f.key().equals("OPENING_DATE")));
    assertTrue(config.stream().anyMatch(f -> f.key().equals("TERM_LENGTH_IN_DAYS")));
  }

  @Test
  void shouldReturnRealEstateConfiguration() {
    final List<MetadataFieldConfigDTO> config =
        registry.getConfigurationFor(ProductType.REAL_ESTATE_INVESTMENT);

    assertNotNull(config);
    assertTrue(config.stream().anyMatch(f -> f.key().equals("REAL_ESTATE_PURCHASE_DATE")));
    assertTrue(config.stream().anyMatch(f -> f.key().equals("REAL_ESTATE_PURCHASE_PRICE")));

    final List<MetadataFieldConfigDTO> optionalFields =
        config.stream().filter(f -> !f.required()).toList();
    assertTrue(optionalFields.stream().anyMatch(f -> f.key().equals("REAL_ESTATE_RENTAL_INCOME")));
  }

  @Test
  void shouldReturnSavingsConfigurationWithOptionalFields() {
    final List<MetadataFieldConfigDTO> config = registry.getConfigurationFor(ProductType.SAVINGS);

    assertNotNull(config);
    final List<MetadataFieldConfigDTO> optionalFields =
        config.stream().filter(f -> !f.required()).toList();
    assertFalse(optionalFields.stream().anyMatch(f -> f.key().equals("COMMON_INITIAL_BALANCE")));
  }

  @Test
  void shouldReturnCorrectValueTypes() {
    final List<MetadataFieldConfigDTO> config = registry.getConfigurationFor(ProductType.LOAN);

    final MetadataFieldConfigDTO principalAmount =
        config.stream()
            .filter(f -> f.key().equals("LOAN_PRINCIPAL_AMOUNT"))
            .findFirst()
            .orElseThrow();

    assertEquals("BIGDECIMAL", principalAmount.valueType());
    assertEquals("0", principalAmount.minValue());
  }

  @Test
  void shouldReturnMinMaxValues() {
    final List<MetadataFieldConfigDTO> config =
        registry.getConfigurationFor(ProductType.CREDIT_CARD);

    final MetadataFieldConfigDTO paymentDueDay =
        config.stream().filter(f -> f.key().equals("PAYMENT_DUE_DAY")).findFirst().orElseThrow();

    assertEquals("INT", paymentDueDay.valueType());
    assertEquals("1", paymentDueDay.minValue());
    assertEquals("31", paymentDueDay.maxValue());
  }
}

package com.jbh.products.application.feature.product.services.metadata;

import static com.jbh.products.domain.product.vo.ProductMetadataKey.BROKER_NAME;
import static com.jbh.products.domain.product.vo.ProductMetadataKey.COMMISSION_RATE;
import static com.jbh.products.domain.product.vo.ProductMetadataKey.COMMON_INITIAL_BALANCE;
import static com.jbh.products.domain.product.vo.ProductMetadataKey.CREDIT_LIMIT;
import static com.jbh.products.domain.product.vo.ProductMetadataKey.LOAN_INTEREST_RATE;
import static com.jbh.products.domain.product.vo.ProductMetadataKey.LOAN_PAYOFF_AMOUNT_TODAY;
import static com.jbh.products.domain.product.vo.ProductMetadataKey.LOAN_PRINCIPAL_AMOUNT;
import static com.jbh.products.domain.product.vo.ProductMetadataKey.LOAN_TOTAL_AMOUNT_PAID;
import static com.jbh.products.domain.product.vo.ProductMetadataKey.MATURITY_DATE;
import static com.jbh.products.domain.product.vo.ProductMetadataKey.OPENING_DATE;
import static com.jbh.products.domain.product.vo.ProductMetadataKey.PAYMENT_DUE_DAY;
import static com.jbh.products.domain.product.vo.ProductMetadataKey.REAL_ESTATE_DOWN_PAYMENT_AMOUNT;
import static com.jbh.products.domain.product.vo.ProductMetadataKey.REAL_ESTATE_DOWN_PAYMENT_PAID_TO_DATE;
import static com.jbh.products.domain.product.vo.ProductMetadataKey.REAL_ESTATE_DOWN_PAYMENT_PERCENTAGE;
import static com.jbh.products.domain.product.vo.ProductMetadataKey.REAL_ESTATE_FINANCED_AMOUNT;
import static com.jbh.products.domain.product.vo.ProductMetadataKey.REAL_ESTATE_PROPERTY_SIZE;
import static com.jbh.products.domain.product.vo.ProductMetadataKey.REAL_ESTATE_PURCHASE_DATE;
import static com.jbh.products.domain.product.vo.ProductMetadataKey.REAL_ESTATE_PURCHASE_PRICE;
import static com.jbh.products.domain.product.vo.ProductMetadataKey.REAL_ESTATE_RENTAL_INCOME;
import static com.jbh.products.domain.product.vo.ProductMetadataKey.TERM_LENGTH_IN_DAYS;

import com.jbh.products.application.feature.product.dto.MetadataFieldConfigDTO;
import com.jbh.products.domain.product.vo.ProductMetadataKey;
import com.jbh.products.domain.product.vo.ProductType;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class ProductMetadataConfigRegistry {

  private static final Map<ProductType, Set<ProductMetadataKey>> REQUIRED_FIELDS =
      Map.of(
          ProductType.LOAN,
              new LinkedHashSet<>(
                  List.of(
                      LOAN_PRINCIPAL_AMOUNT,
                      LOAN_INTEREST_RATE,
                      LOAN_TOTAL_AMOUNT_PAID,
                      LOAN_PAYOFF_AMOUNT_TODAY)),
          ProductType.REAL_ESTATE_INVESTMENT,
              new LinkedHashSet<>(
                  List.of(
                      REAL_ESTATE_PURCHASE_DATE,
                      REAL_ESTATE_PURCHASE_PRICE,
                      REAL_ESTATE_PROPERTY_SIZE,
                      REAL_ESTATE_FINANCED_AMOUNT,
                      REAL_ESTATE_DOWN_PAYMENT_PERCENTAGE,
                      REAL_ESTATE_DOWN_PAYMENT_PAID_TO_DATE)),
          ProductType.CDT,
              new LinkedHashSet<>(
                  List.of(MATURITY_DATE, OPENING_DATE, TERM_LENGTH_IN_DAYS, COMMISSION_RATE)),
          ProductType.INVESTMENT, new LinkedHashSet<>(List.of(BROKER_NAME, COMMISSION_RATE)),
          ProductType.CREDIT_CARD, new LinkedHashSet<>(List.of(CREDIT_LIMIT, PAYMENT_DUE_DAY)),
          ProductType.SAVINGS, new LinkedHashSet<>(List.of(COMMON_INITIAL_BALANCE)));

  private static final Map<ProductType, Set<ProductMetadataKey>> OPTIONAL_FIELDS =
      Map.of(
          ProductType.LOAN, new LinkedHashSet<>(),
          ProductType.REAL_ESTATE_INVESTMENT,
              new LinkedHashSet<>(
                  List.of(REAL_ESTATE_RENTAL_INCOME, REAL_ESTATE_DOWN_PAYMENT_AMOUNT)),
          ProductType.CDT, new LinkedHashSet<>(),
          ProductType.INVESTMENT, new LinkedHashSet<>(),
          ProductType.CREDIT_CARD, new LinkedHashSet<>(),
          ProductType.SAVINGS, new LinkedHashSet<>());

  public List<MetadataFieldConfigDTO> getConfigurationFor(final ProductType productType) {
    final Set<ProductMetadataKey> required = REQUIRED_FIELDS.getOrDefault(productType, Set.of());
    final Set<ProductMetadataKey> optional = OPTIONAL_FIELDS.getOrDefault(productType, Set.of());

    final List<MetadataFieldConfigDTO> result = new ArrayList<>();

    required.forEach(key -> result.add(toDTO(key, true)));
    optional.forEach(key -> result.add(toDTO(key, false)));

    return result;
  }

  private MetadataFieldConfigDTO toDTO(final ProductMetadataKey key, final boolean required) {
    return new MetadataFieldConfigDTO(
        key.name(),
        key.getValueType().name(),
        required,
        formatValue(key.getMinValue()),
        formatValue(key.getMaxValue()),
        key.getDisplayName());
  }

  private String formatValue(final Object value) {
    return value != null ? value.toString() : null;
  }
}

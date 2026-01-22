package com.jbh.account.domain.vo;

import static com.jbh.account.domain.vo.MetadataValueType.BIGDECIMAL;
import static com.jbh.account.domain.vo.MetadataValueType.BOOLEAN;
import static com.jbh.account.domain.vo.MetadataValueType.DATE;
import static com.jbh.account.domain.vo.MetadataValueType.INT;
import static com.jbh.account.domain.vo.MetadataValueType.STRING;

import java.math.BigDecimal;
import java.util.List;

@SuppressWarnings("PMD.LongVariable")
public enum ProductMetadataKey {
  // Credit Card Input Metadata
  CREDIT_LIMIT(BIGDECIMAL, BigDecimal.ZERO, null),
  PAYMENT_DUE_DAY(INT, 1, 31),

  // Investment Input Metadata
  BROKER_NAME(STRING, null, null),
  COMMISSION_RATE(BIGDECIMAL, BigDecimal.ZERO, new BigDecimal("100")),

  // CDT Input Metadata
  MATURITY_DATE(DATE, null, null),
  OPENING_DATE(DATE, null, null),
  TERM_LENGTH_IN_DAYS(INT, 1, 3650),

  // System Calculated Metadata (all types)
  COMMON_INITIAL_BALANCE(BIGDECIMAL, null, null),
  COMMON_IS_FULLY_WITHDRAWN(BOOLEAN, null, null),
  COMMON_FULLY_WITHDRAWN_DATE(DATE, null, null),
  COMMON_FULLY_WITHDRAWN_AT(DATE, null, null),

  // Core Loan Terms
  LOAN_PRINCIPAL_AMOUNT(BIGDECIMAL, BigDecimal.ZERO, null),
  LOAN_INTEREST_RATE(BIGDECIMAL, BigDecimal.ZERO, new BigDecimal("100")),
  LOAN_TOTAL_AMOUNT_PAID(BIGDECIMAL, BigDecimal.ZERO, null),
  LOAN_PAYOFF_AMOUNT_TODAY(BIGDECIMAL, BigDecimal.ZERO, null),

  // Real Estate
  REAL_ESTATE_PURCHASE_DATE(DATE, null, null),
  REAL_ESTATE_PURCHASE_PRICE(BIGDECIMAL, BigDecimal.ZERO, null),
  REAL_ESTATE_PROPERTY_SIZE(BIGDECIMAL, BigDecimal.ZERO, null),
  REAL_ESTATE_RENTAL_INCOME(BIGDECIMAL, null, null),
  REAL_ESTATE_FINANCED_AMOUNT(BIGDECIMAL, BigDecimal.ZERO, null),
  REAL_ESTATE_DOWN_PAYMENT_AMOUNT(BIGDECIMAL, BigDecimal.ZERO, null),
  REAL_ESTATE_DOWN_PAYMENT_PERCENTAGE(BIGDECIMAL, BigDecimal.ZERO, new BigDecimal("100")),
  REAL_ESTATE_DOWN_PAYMENT_PAID_TO_DATE(BIGDECIMAL, BigDecimal.ZERO, null),
  ;

  private static final List<ProductMetadataKey> REQUIRED_LOAN_METADATA =
      List.of(LOAN_PRINCIPAL_AMOUNT, LOAN_TOTAL_AMOUNT_PAID, LOAN_PAYOFF_AMOUNT_TODAY);

  private static final List<ProductMetadataKey> REQUIRED_REAL_ESTATE_METADATA =
      List.of(
          REAL_ESTATE_PURCHASE_DATE,
          REAL_ESTATE_PURCHASE_PRICE,
          REAL_ESTATE_PROPERTY_SIZE,
          REAL_ESTATE_FINANCED_AMOUNT,
          REAL_ESTATE_DOWN_PAYMENT_PERCENTAGE,
          REAL_ESTATE_DOWN_PAYMENT_PAID_TO_DATE);

  private final MetadataValueType valueType;
  private final Object minValue;
  private final Object maxValue;

  ProductMetadataKey(
      final MetadataValueType valueType, final Object minValue, final Object maxValue) {
    this.valueType = valueType;
    this.minValue = minValue;
    this.maxValue = maxValue;
  }

  public MetadataValueType getValueType() {
    return valueType;
  }

  public Object getMinValue() {
    return minValue;
  }

  public Object getMaxValue() {
    return maxValue;
  }

  public static List<ProductMetadataKey> findRequiredMetadataBy(final ProductType productType) {
    return switch (productType) {
      case REAL_ESTATE_INVESTMENT -> REQUIRED_REAL_ESTATE_METADATA;
      case LOAN -> REQUIRED_LOAN_METADATA;
      default -> throw new IllegalStateException("Unexpected value: " + productType);
    };
  }
}

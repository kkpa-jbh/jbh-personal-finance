package com.jbh.products.domain.product.vo;

import static com.jbh.products.domain.shared.vo.MetadataValueType.BIGDECIMAL;
import static com.jbh.products.domain.shared.vo.MetadataValueType.BOOLEAN;
import static com.jbh.products.domain.shared.vo.MetadataValueType.DATE;
import static com.jbh.products.domain.shared.vo.MetadataValueType.INT;
import static com.jbh.products.domain.shared.vo.MetadataValueType.STRING;

import com.jbh.products.domain.shared.vo.MetadataValueType;

import java.math.BigDecimal;
import java.util.List;

@SuppressWarnings("PMD.LongVariable")
public enum ProductMetadataKey {
  // Credit Card Input Metadata
  CREDIT_LIMIT(BIGDECIMAL, BigDecimal.ZERO, null, "Credit Limit", "Límite de Crédito"),
  PAYMENT_DUE_DAY(INT, 1, 31, "Payment Due Day", "Día de Pago"),

  // Investment Input Metadata
  BROKER_NAME(STRING, null, null, "Broker Name", "Nombre del Corredor"),
  COMMISSION_RATE(BIGDECIMAL, BigDecimal.ZERO, new BigDecimal("100"), "Commission Rate", "Tasa de Comisión"),

  // CDT Input Metadata
  MATURITY_DATE(DATE, null, null, "Maturity Date", "Fecha de Vencimiento"),
  OPENING_DATE(DATE, null, null, "Opening Date", "Fecha de Apertura"),
  TERM_LENGTH_IN_DAYS(INT, 1, 3650, "Term Length (Days)", "Plazo (Días)"),

  // System Calculated Metadata (all types)
  COMMON_INITIAL_BALANCE(BIGDECIMAL, null, null, "Initial Balance", "Saldo Inicial"),
  COMMON_IS_FULLY_WITHDRAWN(BOOLEAN, null, null, "Fully Withdrawn", "Retirado Completamente"),
  COMMON_FULLY_WITHDRAWN_DATE(DATE, null, null, "Fully Withdrawn Date", "Fecha de Retiro Completo"),
  COMMON_FULLY_WITHDRAWN_AT(DATE, null, null, "Fully Withdrawn At", "Retirado Completamente En"),

  // Core Loan Terms
  LOAN_PRINCIPAL_AMOUNT(BIGDECIMAL, BigDecimal.ZERO, null, "Principal Amount", "Monto Principal"),
  LOAN_INTEREST_RATE(BIGDECIMAL, BigDecimal.ZERO, new BigDecimal("100"), "Interest Rate", "Tasa de Interés"),
  LOAN_TOTAL_AMOUNT_PAID(BIGDECIMAL, BigDecimal.ZERO, null, "Total Amount Paid", "Monto Total Pagado"),
  LOAN_PAYOFF_AMOUNT_TODAY(BIGDECIMAL, BigDecimal.ZERO, null, "Payoff Amount Today", "Monto de Liquidación Hoy"),

  // Real Estate
  REAL_ESTATE_PURCHASE_DATE(DATE, null, null, "Purchase Date", "Fecha de Compra"),
  REAL_ESTATE_PURCHASE_PRICE(BIGDECIMAL, BigDecimal.ZERO, null, "Purchase Price", "Precio de Compra"),
  REAL_ESTATE_PROPERTY_SIZE(BIGDECIMAL, BigDecimal.ZERO, null, "Property Size", "Tamaño de la Propiedad"),
  REAL_ESTATE_RENTAL_INCOME(BIGDECIMAL, null, null, "Rental Income", "Ingreso por Alquiler"),
  REAL_ESTATE_FINANCED_AMOUNT(BIGDECIMAL, BigDecimal.ZERO, null, "Financed Amount", "Monto Financiado"),
  REAL_ESTATE_DOWN_PAYMENT_AMOUNT(BIGDECIMAL, BigDecimal.ZERO, null, "Down Payment Amount", "Monto de Cuota Inicial"),
  REAL_ESTATE_DOWN_PAYMENT_PERCENTAGE(BIGDECIMAL, BigDecimal.ZERO, new BigDecimal("100"), "Down Payment Percentage", "Porcentaje de Cuota Inicial"),
  REAL_ESTATE_DOWN_PAYMENT_PAID_TO_DATE(BIGDECIMAL, BigDecimal.ZERO, null, "Down Payment Paid to Date", "Cuota Inicial Pagada a la Fecha"),
  ;

  private static final String DISPLAY_NAME_FORMAT = "{\"en\":\"%s\",\"es\":\"%s\"}";

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
  private final String nameEn;
  private final String nameEs;

  ProductMetadataKey(
      final MetadataValueType valueType,
      final Object minValue,
      final Object maxValue,
      final String nameEn,
      final String nameEs) {
    this.valueType = valueType;
    this.minValue = minValue;
    this.maxValue = maxValue;
    this.nameEn = nameEn;
    this.nameEs = nameEs;
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

  public String getNameEn() {
    return nameEn;
  }

  public String getNameEs() {
    return nameEs;
  }

  public String getDisplayName() {
    return String.format(DISPLAY_NAME_FORMAT, nameEn, nameEs);
  }

  public static List<ProductMetadataKey> findRequiredMetadataBy(final ProductType productType) {
    return switch (productType) {
      case REAL_ESTATE_INVESTMENT -> REQUIRED_REAL_ESTATE_METADATA;
      case LOAN -> REQUIRED_LOAN_METADATA;
      default -> throw new IllegalStateException("Unexpected value: " + productType);
    };
  }
}

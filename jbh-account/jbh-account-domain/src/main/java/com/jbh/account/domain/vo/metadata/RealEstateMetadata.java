package com.jbh.account.domain.vo.metadata;

import com.jbh.account.domain.vo.ProductMetadataKey;
import com.jbh.commons.util.JbhMoneyUtils;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Map;

/** Metadata specific to real estate investment products. */
public final class RealEstateMetadata {

  private final Map<ProductMetadataKey, Object> data;

  public RealEstateMetadata(final Map<ProductMetadataKey, Object> data) {
    this.data = data;
  }

  /**
   * Gets the real estate purchase date.
   *
   * @return LocalDate purchase date or null if not set
   */
  public LocalDate getPurchaseDate() {
    return (LocalDate) get(ProductMetadataKey.REAL_ESTATE_PURCHASE_DATE);
  }

  private Object get(final ProductMetadataKey key) {
    return data.get(key);
  }

  /**
   * Gets the real estate purchase price.
   *
   * @return BigDecimal purchase price or ZERO if not set
   */
  public BigDecimal getPurchasePrice() {
    return getDecimal(ProductMetadataKey.REAL_ESTATE_PURCHASE_PRICE);
  }

  private BigDecimal getDecimal(final ProductMetadataKey key) {
    final Object result = get(key);
    return result instanceof BigDecimal ? (BigDecimal) result : BigDecimal.ZERO;
  }

  /**
   * Gets the property size.
   *
   * @return BigDecimal property size or ZERO if not set
   */
  public BigDecimal getPropertySize() {
    return getDecimal(ProductMetadataKey.REAL_ESTATE_PROPERTY_SIZE);
  }

  /**
   * Gets the financed amount.
   *
   * @return BigDecimal financed amount or ZERO if not set
   */
  public BigDecimal getFinancedAmount() {
    return getDecimal(ProductMetadataKey.REAL_ESTATE_FINANCED_AMOUNT);
  }

  /**
   * Gets the down payment amount.
   *
   * @return BigDecimal down payment amount or ZERO if not set
   */
  public BigDecimal getDownPaymentAmount() {
    return getDecimal(ProductMetadataKey.REAL_ESTATE_DOWN_PAYMENT_AMOUNT);
  }

  /**
   * Gets the down payment percentage.
   *
   * @return BigDecimal down payment percentage or ZERO if not set
   */
  public BigDecimal getDownPaymentPercentage() {
    return getDecimal(ProductMetadataKey.REAL_ESTATE_DOWN_PAYMENT_PERCENTAGE);
  }

  /**
   * Gets the down payment paid to date.
   *
   * @return BigDecimal down payment paid to date or ZERO if not set
   */
  public BigDecimal getDownPaymentPaidToDate() {
    return getDecimal(ProductMetadataKey.REAL_ESTATE_DOWN_PAYMENT_PAID_TO_DATE);
  }

  /**
   * Sets the purchase date.
   *
   * @param purchaseDate The purchase date
   */
  public void putPurchaseDate(final LocalDate purchaseDate) {
    put(ProductMetadataKey.REAL_ESTATE_PURCHASE_DATE, purchaseDate);
  }

  private void put(final ProductMetadataKey key, final Object value) {
    if (value instanceof BigDecimal) {
      data.put(key, JbhMoneyUtils.withJBHDecimals((BigDecimal) value));
      return;
    }
    data.put(key, value);
  }

  /**
   * Sets the purchase price.
   *
   * @param purchasePrice The purchase price
   */
  public void putPurchasePrice(final BigDecimal purchasePrice) {
    put(ProductMetadataKey.REAL_ESTATE_PURCHASE_PRICE, purchasePrice);
  }

  /**
   * Sets the property size.
   *
   * @param propertySize The property size
   */
  public void putPropertySize(final BigDecimal propertySize) {
    put(ProductMetadataKey.REAL_ESTATE_PROPERTY_SIZE, propertySize);
  }

  /**
   * Sets the financed amount.
   *
   * @param financedAmount The financed amount
   */
  public void putFinancedAmount(final BigDecimal financedAmount) {
    put(ProductMetadataKey.REAL_ESTATE_FINANCED_AMOUNT, financedAmount);
  }

  /**
   * Sets the down payment amount.
   *
   * @param downPaymentAmount The down payment amount
   */
  public void putDownPaymentAmount(final BigDecimal downPaymentAmount) {
    put(ProductMetadataKey.REAL_ESTATE_DOWN_PAYMENT_AMOUNT, downPaymentAmount);
  }

  /**
   * Sets the down payment percentage.
   *
   * @param downPaymentPercentage The down payment percentage
   */
  public void putDownPaymentPercentage(final BigDecimal downPaymentPercentage) {
    put(ProductMetadataKey.REAL_ESTATE_DOWN_PAYMENT_PERCENTAGE, downPaymentPercentage);
  }

  /**
   * Sets the down payment paid to date.
   *
   * @param downPaymentPaidToDate The down payment paid to date
   */
  public void putDownPaymentPaidToDate(final BigDecimal downPaymentPaidToDate) {
    put(ProductMetadataKey.REAL_ESTATE_DOWN_PAYMENT_PAID_TO_DATE, downPaymentPaidToDate);
  }
}

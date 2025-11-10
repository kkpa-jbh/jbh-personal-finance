package com.jbh.account.domain.vo;

import com.jbh.account.domain.utils.JbhBooleanUtils;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;

/**
 * Value Object that encapsulates account metadata and provides type-safe access to metadata fields.
 * This VO can be used across all layers (domain, application, infrastructure) in the hexagonal
 * architecture without breaking layer isolation.
 */
public final class ProductMetadata {

  private final Map<ProductMetadataKey, Object> data;

  private ProductMetadata(final Map<ProductMetadataKey, Object> data) {
    this.data =
        data != null && !data.isEmpty()
            ? new EnumMap<>(data)
            : new EnumMap<>(ProductMetadataKey.class);
  }

  /**
   * Creates an empty AccountMetadata instance.
   *
   * @return AccountMetadata with no data
   */
  public static ProductMetadata empty() {
    return new ProductMetadata(new EnumMap<>(ProductMetadataKey.class));
  }

  public static ProductMetadata of(final Map<ProductMetadataKey, Object> data) {
    return new ProductMetadata(data);
  }

  /**
   * Checks if the account is fully withdrawn based on metadata.
   *
   * @return true if FULLY_WITHDRAWN key exists and is true
   */
  public boolean isFullyWithdrawn() {
    return hasKey(ProductMetadataKey.IS_FULLY_WITHDRAWN)
        && JbhBooleanUtils.isTrue(get(ProductMetadataKey.IS_FULLY_WITHDRAWN));
  }

  /**
   * Checks if a specific metadata key exists.
   *
   * @param key The metadata key to check
   * @return true if the key exists in metadata
   */
  public boolean hasKey(final ProductMetadataKey key) {
    return data.containsKey(key);
  }

  /**
   * Retrieves the value for a specific metadata key.
   *
   * @param key The metadata key
   * @return The value associated with the key, or null if not present
   */
  public Object get(final ProductMetadataKey key) {
    return data.get(key);
  }

  /**
   * Gets the date when the account was fully withdrawn.
   *
   * @return LocalDate or null if not set
   */
  public LocalDate getFullyWithdrawnDate() {
    return (LocalDate) get(ProductMetadataKey.FULLY_WITHDRAWN_DATE);
  }

  // ========== Typed Getters for Fully Withdrawn Data ==========

  /**
   * Gets the timestamp when the account was marked as fully withdrawn.
   *
   * @return LocalDateTime or null if not set
   */
  public LocalDateTime getFullyWithdrawnAt() {
    return (LocalDateTime) get(ProductMetadataKey.FULLY_WITHDRAWN_AT);
  }

  /**
   * Gets the credit limit for credit card accounts.
   *
   * @return BigDecimal credit limit or null if not set
   */
  public BigDecimal getCreditLimit() {
    return (BigDecimal) get(ProductMetadataKey.CREDIT_LIMIT);
  }

  // ========== Typed Getters for Credit Card Metadata ==========

  /**
   * Gets the payment due day for credit card accounts.
   *
   * @return Integer day of month or null if not set
   */
  public Integer getPaymentDueDay() {
    return (Integer) get(ProductMetadataKey.PAYMENT_DUE_DAY);
  }

  /**
   * Gets the broker name for investment accounts.
   *
   * @return String broker name or null if not set
   */
  public String getBrokerName() {
    return (String) get(ProductMetadataKey.BROKER_NAME);
  }

  // ========== Typed Getters for Investment Metadata ==========

  /**
   * Gets the commission rate for investment accounts.
   *
   * @return BigDecimal commission rate or null if not set
   */
  public BigDecimal getCommissionRate() {
    return (BigDecimal) get(ProductMetadataKey.COMMISSION_RATE);
  }

  /**
   * Gets the maturity date for CDT accounts.
   *
   * @return LocalDate maturity date or null if not set
   */
  public LocalDate getMaturityDate() {
    return (LocalDate) get(ProductMetadataKey.MATURITY_DATE);
  }

  // ========== Typed Getters for CDT Metadata ==========

  /**
   * Gets the opening date for CDT accounts.
   *
   * @return LocalDate opening date or null if not set
   */
  public LocalDate getOpeningDate() {
    return (LocalDate) get(ProductMetadataKey.OPENING_DATE);
  }

  /**
   * Gets the term length in days for CDT accounts.
   *
   * @return Integer term length or null if not set
   */
  public Integer getTermLengthInDays() {
    return (Integer) get(ProductMetadataKey.TERM_LENGTH_IN_DAYS);
  }

  /**
   * Returns a defensive copy of the underlying metadata map with String keys. Used for persistence
   * and serialization.
   *
   * @return A new HashMap containing all metadata entries with String keys
   */
  public Map<String, Object> asMap() {
    final Map<String, Object> result = new HashMap<>();
    for (final Map.Entry<ProductMetadataKey, Object> entry : data.entrySet()) {
      result.put(entry.getKey().name(), entry.getValue());
    }
    return result;
  }

  public Map<ProductMetadataKey, Object> getData() {
    return new EnumMap<>(data);
  }

  // ========== Map Conversion ==========

  /**
   * Returns the number of metadata entries.
   *
   * @return Size of metadata
   */
  public int size() {
    return data.size();
  }

  /**
   * Checks if metadata is empty.
   *
   * @return true if no metadata entries exist
   */
  public boolean isEmpty() {
    return data.isEmpty();
  }

  public void putFullyWithdrawn(final LocalDate movementDate) {
    put(ProductMetadataKey.FULLY_WITHDRAWN_DATE, movementDate);
    put(ProductMetadataKey.FULLY_WITHDRAWN_AT, LocalDateTime.now());
    put(ProductMetadataKey.IS_FULLY_WITHDRAWN, true);
  }

  /**
   * Adds or updates a metadata entry.
   *
   * @param key The metadata key
   * @param value The value to store
   */
  private void put(final ProductMetadataKey key, final Object value) {
    data.put(key, value);
  }

  public void putInitialBalance(final BigDecimal initialBalance) {
    put(ProductMetadataKey.INITIAL_BALANCE, initialBalance);
  }

  public BigDecimal getInitialBalance() {
    return (BigDecimal) get(ProductMetadataKey.INITIAL_BALANCE);
  }
}

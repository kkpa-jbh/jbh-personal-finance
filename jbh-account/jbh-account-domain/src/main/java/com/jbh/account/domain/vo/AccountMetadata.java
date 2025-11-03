package com.jbh.account.domain.vo;

import com.jbh.account.domain.utils.JbhBooleanUtils;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * Value Object that encapsulates account metadata and provides type-safe access to metadata fields.
 * This VO can be used across all layers (domain, application, infrastructure) in the hexagonal
 * architecture without breaking layer isolation.
 */
public final class AccountMetadata {

  private final Map<String, Object> data;

  private AccountMetadata(final Map<String, Object> data) {
    this.data = data != null ? new HashMap<>(data) : new HashMap<>();
  }

  /**
   * Creates an empty AccountMetadata instance.
   *
   * @return AccountMetadata with no data
   */
  public static AccountMetadata empty() {
    return new AccountMetadata(new HashMap<>());
  }

  /**
   * Creates an AccountMetadata instance from a Map.
   *
   * @param data The metadata map
   * @return AccountMetadata wrapping the provided data
   */
  public static AccountMetadata of(final Map<String, Object> data) {
    return new AccountMetadata(data);
  }

  /**
   * Checks if the account is fully withdrawn based on metadata.
   *
   * @return true if FULLY_WITHDRAWN key exists and is true
   */
  public boolean isFullyWithdrawn() {
    return hasKey(AccountMetadataKey.IS_FULLY_WITHDRAWN)
        && JbhBooleanUtils.isTrue(get(AccountMetadataKey.IS_FULLY_WITHDRAWN));
  }

  /**
   * Checks if a specific metadata key exists.
   *
   * @param key The metadata key to check
   * @return true if the key exists in metadata
   */
  public boolean hasKey(final AccountMetadataKey key) {
    return data.containsKey(key.name());
  }

  /**
   * Retrieves the value for a specific metadata key.
   *
   * @param key The metadata key
   * @return The value associated with the key, or null if not present
   */
  public Object get(final AccountMetadataKey key) {
    return data.get(key.name());
  }

  /**
   * Gets the date when the account was fully withdrawn.
   *
   * @return LocalDate or null if not set
   */
  public LocalDate getFullyWithdrawnDate() {
    return (LocalDate) get(AccountMetadataKey.FULLY_WITHDRAWN_DATE);
  }

  // ========== Typed Getters for Fully Withdrawn Data ==========

  /**
   * Gets the timestamp when the account was marked as fully withdrawn.
   *
   * @return LocalDateTime or null if not set
   */
  public LocalDateTime getFullyWithdrawnAt() {
    return (LocalDateTime) get(AccountMetadataKey.FULLY_WITHDRAWN_AT);
  }

  /**
   * Gets the credit limit for credit card accounts.
   *
   * @return BigDecimal credit limit or null if not set
   */
  public BigDecimal getCreditLimit() {
    return (BigDecimal) get(AccountMetadataKey.CREDIT_LIMIT);
  }

  // ========== Typed Getters for Credit Card Metadata ==========

  /**
   * Gets the payment due day for credit card accounts.
   *
   * @return Integer day of month or null if not set
   */
  public Integer getPaymentDueDay() {
    return (Integer) get(AccountMetadataKey.PAYMENT_DUE_DAY);
  }

  /**
   * Gets the broker name for investment accounts.
   *
   * @return String broker name or null if not set
   */
  public String getBrokerName() {
    return (String) get(AccountMetadataKey.BROKER_NAME);
  }

  // ========== Typed Getters for Investment Metadata ==========

  /**
   * Gets the commission rate for investment accounts.
   *
   * @return BigDecimal commission rate or null if not set
   */
  public BigDecimal getCommissionRate() {
    return (BigDecimal) get(AccountMetadataKey.COMMISSION_RATE);
  }

  /**
   * Gets the maturity date for CDT accounts.
   *
   * @return LocalDate maturity date or null if not set
   */
  public LocalDate getMaturityDate() {
    return (LocalDate) get(AccountMetadataKey.MATURITY_DATE);
  }

  // ========== Typed Getters for CDT Metadata ==========

  /**
   * Gets the opening date for CDT accounts.
   *
   * @return LocalDate opening date or null if not set
   */
  public LocalDate getOpeningDate() {
    return (LocalDate) get(AccountMetadataKey.OPENING_DATE);
  }

  /**
   * Gets the term length in days for CDT accounts.
   *
   * @return Integer term length or null if not set
   */
  public Integer getTermLengthInDays() {
    return (Integer) get(AccountMetadataKey.TERM_LENGTH_IN_DAYS);
  }

  /**
   * Returns a defensive copy of the underlying metadata map. Used for persistence and
   * serialization.
   *
   * @return A new HashMap containing all metadata entries
   */
  public Map<String, Object> asMap() {
    return new HashMap<>(data);
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
    put(AccountMetadataKey.FULLY_WITHDRAWN_DATE, movementDate);
    put(AccountMetadataKey.FULLY_WITHDRAWN_AT, LocalDateTime.now());
    put(AccountMetadataKey.IS_FULLY_WITHDRAWN, true);
  }

  /**
   * Adds or updates a metadata entry.
   *
   * @param key The metadata key
   * @param value The value to store
   */
  private void put(final AccountMetadataKey key, final Object value) {
    data.put(key.name(), value);
  }

  public void putInitialBalance(final BigDecimal initialBalance) {
    put(AccountMetadataKey.INITIAL_BALANCE, initialBalance);
  }

  public BigDecimal getInitialBalance() {
    return (BigDecimal) get(AccountMetadataKey.INITIAL_BALANCE);
  }
}

package com.jbh.account.domain.vo.metadata;

import com.jbh.account.domain.vo.ProductMetadataKey;
import com.jbh.commons.util.JbhBooleanUtils;
import com.jbh.commons.util.JbhMoneyUtils;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Map;

/**
 * Common metadata shared across all product types. Handles fully withdrawn status and initial
 * balance information.
 */
public final class CommonMetadata {

  private final Map<ProductMetadataKey, Object> data;

  public CommonMetadata(final Map<ProductMetadataKey, Object> data) {
    this.data = data;
  }

  /**
   * Checks if the account is fully withdrawn based on metadata.
   *
   * @return true if FULLY_WITHDRAWN key exists and is true
   */
  public boolean isFullyWithdrawn() {
    return hasKey(ProductMetadataKey.COMMON_IS_FULLY_WITHDRAWN)
        && JbhBooleanUtils.isTrue(get(ProductMetadataKey.COMMON_IS_FULLY_WITHDRAWN));
  }

  private boolean hasKey(final ProductMetadataKey key) {
    return data.containsKey(key);
  }

  private Object get(final ProductMetadataKey key) {
    return data.get(key);
  }

  /**
   * Gets the date when the account was fully withdrawn.
   *
   * @return LocalDate or null if not set
   */
  public LocalDate getFullyWithdrawnDate() {
    return (LocalDate) get(ProductMetadataKey.COMMON_FULLY_WITHDRAWN_DATE);
  }

  /**
   * Gets the timestamp when the account was marked as fully withdrawn.
   *
   * @return LocalDateTime or null if not set
   */
  public LocalDateTime getFullyWithdrawnAt() {
    return (LocalDateTime) get(ProductMetadataKey.COMMON_FULLY_WITHDRAWN_AT);
  }

  /**
   * Gets the initial balance.
   *
   * @return BigDecimal initial balance or ZERO if not set
   */
  public BigDecimal getInitialBalance() {
    return getDecimal(ProductMetadataKey.COMMON_INITIAL_BALANCE);
  }

  private BigDecimal getDecimal(final ProductMetadataKey key) {
    final Object result = get(key);
    return JbhMoneyUtils.toDecimal(result);
  }

  /**
   * Marks the account as fully withdrawn with the given movement date.
   *
   * @param movementDate The date of the withdrawal movement
   */
  public void putFullyWithdrawn(final LocalDate movementDate) {
    put(ProductMetadataKey.COMMON_FULLY_WITHDRAWN_DATE, movementDate);
    put(ProductMetadataKey.COMMON_FULLY_WITHDRAWN_AT, LocalDateTime.now());
    put(ProductMetadataKey.COMMON_IS_FULLY_WITHDRAWN, true);
  }

  private void put(final ProductMetadataKey key, final Object value) {
    if (value instanceof BigDecimal) {
      data.put(key, JbhMoneyUtils.withJBHDecimals((BigDecimal) value));
      return;
    }
    data.put(key, value);
  }

  /**
   * Sets the fully withdrawn flag.
   *
   * @param isFullyWithdrawn The fully withdrawn status
   */
  public void putIsFullyWithdrawn(final Boolean isFullyWithdrawn) {
    put(ProductMetadataKey.COMMON_IS_FULLY_WITHDRAWN, isFullyWithdrawn);
  }

  /**
   * Sets the initial balance.
   *
   * @param initialBalance The initial balance
   */
  public void putInitialBalance(final BigDecimal initialBalance) {
    put(ProductMetadataKey.COMMON_INITIAL_BALANCE, initialBalance);
  }
}

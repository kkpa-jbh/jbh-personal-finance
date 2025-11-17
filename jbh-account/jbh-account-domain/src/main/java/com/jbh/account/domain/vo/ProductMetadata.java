package com.jbh.account.domain.vo;

import com.jbh.account.domain.exceptions.AccountBusinessException;
import com.jbh.account.domain.exceptions.BusinessDomainExceptionType;
import com.jbh.account.domain.utils.JbhBooleanUtils;
import com.jbh.account.domain.utils.JbhMoneyUtils;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.EnumMap;
import java.util.Map;

/**
 * Value Object that encapsulates account metadata and provides type-safe access to metadata fields.
 * This VO can be used across all layers (domain, application, infrastructure) in the hexagonal
 * architecture without breaking layer isolation.
 */
public final class ProductMetadata {

  private final Map<ProductMetadataKey, Object> data;

  private ProductMetadata(final Map<ProductMetadataKey, Object> data) {
    this.data = syncTypes(data);
  }

  private Map<ProductMetadataKey, Object> syncTypes(final Map<ProductMetadataKey, Object> data) {
    if (data == null || data.isEmpty()) {
      return new EnumMap<>(ProductMetadataKey.class);
    }

    final Map<ProductMetadataKey, Object> result = new EnumMap<>(ProductMetadataKey.class);

    data.forEach(
        (key, value) -> {
          if (value instanceof BigDecimal) {
            result.put(key, JbhMoneyUtils.withJBHDecimals((BigDecimal) value));
          } else {
            result.put(key, value);
          }
        });

    return result;
  }

  /**
   * Creates an empty AccountMetadata instance.
   *
   * @return AccountMetadata with no data
   */
  public static ProductMetadata empty() {
    return new ProductMetadata(new EnumMap<>(ProductMetadataKey.class));
  }

  /**
   * Creates ProductMetadata from a map. This is primarily used by the infrastructure layer for
   * persistence operations.
   *
   * @param data Map containing metadata
   * @return ProductMetadata instance
   */
  // FIXME: Consider removing this method to enforce immutability
  public static ProductMetadata fromMap(final Map<ProductMetadataKey, Object> data) {
    return new ProductMetadata(data);
  }

  // FIXME: Consider removing this method to enforce immutability
  /**
   * Returns a defensive copy of the underlying metadata map with String keys. Used for persistence
   * and serialization.
   *
   * @return A new HashMap containing all metadata entries with String keys
   */
  public Map<ProductMetadataKey, Object> asMap() {
    return new EnumMap<>(data);
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
  private Object get(final ProductMetadataKey key) {
    return data.get(key);
  }

  // ========== Typed Getters for Fully Withdrawn Data ==========

  /**
   * Gets the date when the account was fully withdrawn.
   *
   * @return LocalDate or null if not set
   */
  public LocalDate getFullyWithdrawnDate() {
    return (LocalDate) get(ProductMetadataKey.FULLY_WITHDRAWN_DATE);
  }

  /**
   * Gets the timestamp when the account was marked as fully withdrawn.
   *
   * @return LocalDateTime or null if not set
   */
  public LocalDateTime getFullyWithdrawnAt() {
    return (LocalDateTime) get(ProductMetadataKey.FULLY_WITHDRAWN_AT);
  }

  // ========== Typed Getters for Credit Card Metadata ==========

  /**
   * Gets the credit limit for credit card accounts.
   *
   * @return BigDecimal credit limit or null if not set
   */
  public BigDecimal getCreditLimit() throws AccountBusinessException {
    final Object creditLimit = get(ProductMetadataKey.CREDIT_LIMIT);
    if (!(creditLimit instanceof BigDecimal)) {
      throw new AccountBusinessException(BusinessDomainExceptionType.INVALID_CREDIT_LIMIT_TYPE);
    }

    return (BigDecimal) creditLimit;
  }

  /**
   * Gets the payment due day for credit card accounts.
   *
   * @return Integer day of month or null if not set
   */
  public Integer getPaymentDueDay() throws AccountBusinessException {
    final Object dueDay = get(ProductMetadataKey.PAYMENT_DUE_DAY);
    if (!(dueDay instanceof Integer)) {
      throw new AccountBusinessException(BusinessDomainExceptionType.INVALID_PAYMENT_DUE_DAY_TYPE);
    }
    return (Integer) dueDay;
  }

  // ========== Typed Getters for Investment Metadata ==========

  /**
   * Gets the broker name for investment accounts.
   *
   * @return String broker name or null if not set
   */
  public String getBrokerName() {
    return (String) get(ProductMetadataKey.BROKER_NAME);
  }

  /**
   * Gets the commission rate for investment accounts.
   *
   * @return BigDecimal commission rate or null if not set
   */
  public BigDecimal getCommissionRate() {
    return getDecimal(ProductMetadataKey.COMMISSION_RATE);
  }

  // ========== Typed Getters for CDT Metadata ==========

  private BigDecimal getDecimal(final ProductMetadataKey key) {
    final Object result = get(key);
    return result instanceof BigDecimal ? (BigDecimal) result : BigDecimal.ZERO;
  }

  /**
   * Gets the maturity date for CDT accounts.
   *
   * @return LocalDate maturity date or null if not set
   */
  public LocalDate getMaturityDate() throws AccountBusinessException {
    final Object maturityDate = get(ProductMetadataKey.MATURITY_DATE);
    if (!(maturityDate instanceof LocalDate)) {
      throw new AccountBusinessException(BusinessDomainExceptionType.INVALID_MATURITY_DATE_TYPE);
    }
    return (LocalDate) maturityDate;
  }

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
    if (value instanceof BigDecimal) {
      data.put(key, JbhMoneyUtils.withJBHDecimals((BigDecimal) value));
    }
    data.put(key, value);
  }

  public void putInitialBalance(final BigDecimal initialBalance) {
    put(ProductMetadataKey.INITIAL_BALANCE, initialBalance);
  }

  // ========== PUT Methods for Credit Card Metadata ==========

  public BigDecimal getInitialBalance() {
    return getDecimal(ProductMetadataKey.INITIAL_BALANCE);
  }

  public void putCreditLimit(final BigDecimal creditLimit) {
    put(ProductMetadataKey.CREDIT_LIMIT, creditLimit);
  }

  // ========== PUT Methods for Investment Metadata ==========

  public void putPaymentDueDay(final Integer paymentDueDay) {
    put(ProductMetadataKey.PAYMENT_DUE_DAY, paymentDueDay);
  }

  public void putBrokerName(final String brokerName) {
    put(ProductMetadataKey.BROKER_NAME, brokerName);
  }

  // ========== PUT Methods for CDT Metadata ==========

  public void putCommissionRate(final BigDecimal commissionRate) {
    put(ProductMetadataKey.COMMISSION_RATE, commissionRate);
  }

  public void putMaturityDate(final LocalDate maturityDate) {
    put(ProductMetadataKey.MATURITY_DATE, maturityDate);
  }

  public void putOpeningDate(final LocalDate openingDate) {
    put(ProductMetadataKey.OPENING_DATE, openingDate);
  }

  // ========== PUT Methods for Fully Withdrawn System Metadata ==========

  public void putTermLengthInDays(final Integer termLengthInDays) {
    put(ProductMetadataKey.TERM_LENGTH_IN_DAYS, termLengthInDays);
  }

  // ========== PUT Methods for Loan Metadata ==========

  public void putIsFullyWithdrawn(final Boolean isFullyWithdrawn) {
    put(ProductMetadataKey.IS_FULLY_WITHDRAWN, isFullyWithdrawn);
  }

  public void putLoanPrincipalAmount(final BigDecimal loanPrincipalAmount) {
    put(ProductMetadataKey.LOAN_PRINCIPAL_AMOUNT, loanPrincipalAmount);
  }

  public void putLoanInterestRate(final BigDecimal loanInterestRate) {
    put(ProductMetadataKey.LOAN_INTEREST_RATE, loanInterestRate);
  }

  public void putLoanTotalAmountPaid(final BigDecimal loanTotalAmountPaid) {
    put(ProductMetadataKey.LOAN_TOTAL_AMOUNT_PAID, loanTotalAmountPaid);
  }

  // ========== Factory Method for Infrastructure Layer ==========

  public void putLoanPayoffAmountToday(final BigDecimal loanPayoffAmountToday) {
    put(ProductMetadataKey.LOAN_PAYOFF_AMOUNT_TODAY, loanPayoffAmountToday);
  }

  public BigDecimal getLoanPrincipalAmount() {
    return getDecimal(ProductMetadataKey.LOAN_PRINCIPAL_AMOUNT);
  }

  public BigDecimal getLoanPayoffAmountToday() {
    return getDecimal(ProductMetadataKey.LOAN_PAYOFF_AMOUNT_TODAY);
  }

  public BigDecimal getLoanTotalAmountPaid() {
    return getDecimal(ProductMetadataKey.LOAN_TOTAL_AMOUNT_PAID);
  }

  public Map<ProductMetadataKey, Object> getData() {
    return new EnumMap<>(data);
  }
}

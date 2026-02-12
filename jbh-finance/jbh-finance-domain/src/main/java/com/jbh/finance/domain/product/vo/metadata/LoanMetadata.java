package com.jbh.finance.domain.product.vo.metadata;

import com.jbh.commons.util.JbhMoneyUtils;
import com.jbh.finance.domain.product.vo.ProductMetadataKey;
import java.math.BigDecimal;
import java.util.Map;

/** Metadata specific to loan products. */
public final class LoanMetadata extends BaseMetadata {

  private final Map<ProductMetadataKey, Object> data;

  public LoanMetadata(final Map<ProductMetadataKey, Object> data) {
    this.data = data;
  }

  /**
   * Gets the loan principal amount (original amount borrowed).
   *
   * @return BigDecimal principal amount or ZERO if not set
   */
  public BigDecimal getPrincipalAmount() {
    return getDecimal(ProductMetadataKey.LOAN_PRINCIPAL_AMOUNT);
  }

  @Override
  protected Object get(final ProductMetadataKey key) {
    return data.get(key);
  }

  /**
   * Gets the loan interest rate (APR).
   *
   * @return BigDecimal interest rate or ZERO if not set
   */
  public BigDecimal getInterestRate() {
    return getDecimal(ProductMetadataKey.LOAN_INTEREST_RATE);
  }

  /**
   * Gets the total cumulative amount paid.
   *
   * @return BigDecimal total amount paid or ZERO if not set
   */
  public BigDecimal getTotalAmountPaid() {
    return getDecimal(ProductMetadataKey.LOAN_TOTAL_AMOUNT_PAID);
  }

  /**
   * Gets the total amount to pay off loan today.
   *
   * @return BigDecimal payoff amount or ZERO if not set
   */
  public BigDecimal getPayoffAmountToday() {
    return getDecimal(ProductMetadataKey.LOAN_PAYOFF_AMOUNT_TODAY);
  }

  /**
   * Sets the principal amount.
   *
   * @param principalAmount The principal amount
   */
  public void putPrincipalAmount(final BigDecimal principalAmount) {
    put(ProductMetadataKey.LOAN_PRINCIPAL_AMOUNT, principalAmount);
  }

  private void put(final ProductMetadataKey key, final Object value) {
    if (value instanceof BigDecimal) {
      data.put(key, JbhMoneyUtils.withJBHDecimals((BigDecimal) value));
      return;
    }
    data.put(key, value);
  }

  /**
   * Sets the interest rate.
   *
   * @param interestRate The interest rate
   */
  public void putInterestRate(final BigDecimal interestRate) {
    put(ProductMetadataKey.LOAN_INTEREST_RATE, interestRate);
  }

  /**
   * Sets the total amount paid.
   *
   * @param totalAmountPaid The total amount paid
   */
  public void putTotalAmountPaid(final BigDecimal totalAmountPaid) {
    put(ProductMetadataKey.LOAN_TOTAL_AMOUNT_PAID, totalAmountPaid);
  }

  /**
   * Sets the payoff amount today.
   *
   * @param payoffAmountToday The payoff amount today
   */
  public void putPayoffAmountToday(final BigDecimal payoffAmountToday) {
    put(ProductMetadataKey.LOAN_PAYOFF_AMOUNT_TODAY, payoffAmountToday);
  }
}

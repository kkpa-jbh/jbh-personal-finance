package com.jbh.account.domain.vo.metadata;

import com.jbh.account.domain.exceptions.ProductBusinessException;
import com.jbh.account.domain.exceptions.BusinessDomainExceptionType;
import com.jbh.account.domain.utils.JbhMoneyUtils;
import com.jbh.account.domain.vo.ProductMetadataKey;
import java.math.BigDecimal;
import java.util.Map;

/** Metadata specific to credit card products. */
public final class CreditCardMetadata {

  private final Map<ProductMetadataKey, Object> data;

  public CreditCardMetadata(final Map<ProductMetadataKey, Object> data) {
    this.data = data;
  }

  /**
   * Gets the credit limit for credit card accounts.
   *
   * @return BigDecimal credit limit
   * @throws ProductBusinessException if credit limit is not a BigDecimal
   */
  public BigDecimal getCreditLimit() throws ProductBusinessException {
    final Object creditLimit = get(ProductMetadataKey.CREDIT_LIMIT);
    if (!(creditLimit instanceof BigDecimal)) {
      throw new ProductBusinessException(BusinessDomainExceptionType.INVALID_CREDIT_LIMIT_TYPE);
    }
    return (BigDecimal) creditLimit;
  }

  /**
   * Gets the payment due day for credit card accounts.
   *
   * @return Integer day of month
   * @throws ProductBusinessException if payment due day is not an Integer
   */
  public Integer getPaymentDueDay() throws ProductBusinessException {
    final Object dueDay = get(ProductMetadataKey.PAYMENT_DUE_DAY);
    if (!(dueDay instanceof Integer)) {
      throw new ProductBusinessException(BusinessDomainExceptionType.INVALID_PAYMENT_DUE_DAY_TYPE);
    }
    return (Integer) dueDay;
  }

  /**
   * Sets the credit limit.
   *
   * @param creditLimit The credit limit
   */
  public void putCreditLimit(final BigDecimal creditLimit) {
    put(ProductMetadataKey.CREDIT_LIMIT, creditLimit);
  }

  /**
   * Sets the payment due day.
   *
   * @param paymentDueDay The payment due day (day of month)
   */
  public void putPaymentDueDay(final Integer paymentDueDay) {
    put(ProductMetadataKey.PAYMENT_DUE_DAY, paymentDueDay);
  }

  private Object get(final ProductMetadataKey key) {
    return data.get(key);
  }

  private void put(final ProductMetadataKey key, final Object value) {
    if (value instanceof BigDecimal) {
      data.put(key, JbhMoneyUtils.withJBHDecimals((BigDecimal) value));
      return;
    }
    data.put(key, value);
  }
}

package com.jbh.products.domain.product.vo.metadata;

import com.jbh.commons.exception.BusinessException;
import com.jbh.commons.util.JbhMoneyUtils;
import com.jbh.products.domain.product.vo.ProductMetadataKey;
import com.jbh.products.domain.shared.exceptions.BusinessDomainExceptionType;
import java.math.BigDecimal;
import java.util.Map;

/** Metadata specific to credit card products. */
public final class CreditCardMetadata extends BaseMetadata {

  private final Map<ProductMetadataKey, Object> data;

  public CreditCardMetadata(final Map<ProductMetadataKey, Object> data) {
    this.data = data;
  }

  /**
   * Gets the credit limit for credit card products
   *
   * @return BigDecimal credit limit
   * @throws BusinessException if credit limit is not a BigDecimal
   */
  public BigDecimal getCreditLimit() throws BusinessException {
    final Object creditLimit = get(ProductMetadataKey.CREDIT_LIMIT);

    if (creditLimit == null) {
      return null;
    }

    if (JbhMoneyUtils.isNotNumber(creditLimit)) {
      throw new BusinessException(BusinessDomainExceptionType.INVALID_CREDIT_LIMIT_TYPE);
    }
    return JbhMoneyUtils.toJBHDecimal(creditLimit);
  }

  @Override
  protected Object get(final ProductMetadataKey key) {
    return data.get(key);
  }

  /**
   * Gets the payment due day for credit card products
   *
   * @return Integer day of month
   * @throws BusinessException if payment due day is not an Integer
   */
  public Integer getPaymentDueDay() throws BusinessException {
    final Object dueDay = get(ProductMetadataKey.PAYMENT_DUE_DAY);
    if (dueDay != null && !(dueDay instanceof Integer)) {
      throw new BusinessException(BusinessDomainExceptionType.INVALID_PAYMENT_DUE_DAY_TYPE);
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

  private void put(final ProductMetadataKey key, final Object value) {
    if (value instanceof BigDecimal) {
      data.put(key, JbhMoneyUtils.withJBHDecimals((BigDecimal) value));
      return;
    }
    data.put(key, value);
  }

  /**
   * Sets the payment due day.
   *
   * @param paymentDueDay The payment due day (day of month)
   */
  public void putPaymentDueDay(final Integer paymentDueDay) {
    put(ProductMetadataKey.PAYMENT_DUE_DAY, paymentDueDay);
  }
}

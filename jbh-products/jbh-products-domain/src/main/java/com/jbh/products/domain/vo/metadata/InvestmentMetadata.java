package com.jbh.products.domain.vo.metadata;

import com.jbh.commons.util.JbhMoneyUtils;
import com.jbh.products.domain.vo.ProductMetadataKey;
import java.math.BigDecimal;
import java.util.Map;

/** Metadata specific to investment products. */
public final class InvestmentMetadata extends BaseMetadata {

  private final Map<ProductMetadataKey, Object> data;

  public InvestmentMetadata(final Map<ProductMetadataKey, Object> data) {
    this.data = data;
  }

  /**
   * Gets the broker name for investment products
   *
   * @return String broker name or null if not set
   */
  public String getBrokerName() {
    return (String) get(ProductMetadataKey.BROKER_NAME);
  }

  @Override
  protected Object get(final ProductMetadataKey key) {
    return data.get(key);
  }

  /**
   * Gets the commission rate for investment products
   *
   * @return BigDecimal commission rate or ZERO if not set
   */
  public BigDecimal getCommissionRate() {
    return getDecimal(ProductMetadataKey.COMMISSION_RATE);
  }

  /**
   * Sets the broker name.
   *
   * @param brokerName The broker name
   */
  public void putBrokerName(final String brokerName) {
    put(ProductMetadataKey.BROKER_NAME, brokerName);
  }

  private void put(final ProductMetadataKey key, final Object value) {
    if (value instanceof BigDecimal) {
      data.put(key, JbhMoneyUtils.withJBHDecimals((BigDecimal) value));
      return;
    }
    data.put(key, value);
  }

  /**
   * Sets the commission rate.
   *
   * @param commissionRate The commission rate
   */
  public void putCommissionRate(final BigDecimal commissionRate) {
    put(ProductMetadataKey.COMMISSION_RATE, commissionRate);
  }
}

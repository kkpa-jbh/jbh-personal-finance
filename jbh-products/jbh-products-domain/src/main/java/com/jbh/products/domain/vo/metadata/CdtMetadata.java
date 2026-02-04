package com.jbh.products.domain.vo.metadata;

import com.jbh.commons.exception.BusinessException;
import com.jbh.commons.util.JbhMoneyUtils;
import com.jbh.products.domain.exceptions.BusinessDomainExceptionType;
import com.jbh.products.domain.vo.ProductMetadataKey;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Map;

/** Metadata specific to CDT (Certificate of Deposit) products. */
public final class CdtMetadata extends BaseMetadata {

  private final Map<ProductMetadataKey, Object> data;

  public CdtMetadata(final Map<ProductMetadataKey, Object> data) {
    this.data = data;
  }

  /**
   * Gets the maturity date for CDT products
   *
   * @return LocalDate maturity date
   * @throws BusinessException if maturity date is not a LocalDate
   */
  public LocalDate getMaturityDate() throws BusinessException {
    final Object maturityDate = get(ProductMetadataKey.MATURITY_DATE);
    if (!(maturityDate instanceof LocalDate)) {
      throw new BusinessException(BusinessDomainExceptionType.INVALID_MATURITY_DATE_TYPE);
    }
    return (LocalDate) maturityDate;
  }

  @Override
  protected Object get(final ProductMetadataKey key) {
    return data.get(key);
  }

  /**
   * Gets the opening date for CDT products
   *
   * @return LocalDate opening date or null if not set
   */
  public LocalDate getOpeningDate() {
    return (LocalDate) get(ProductMetadataKey.OPENING_DATE);
  }

  /**
   * Gets the term length in days for CDT products
   *
   * @return Integer term length or null if not set
   */
  public Integer getTermLengthInDays() {
    return (Integer) get(ProductMetadataKey.TERM_LENGTH_IN_DAYS);
  }

  /**
   * Gets the commission rate (if applicable).
   *
   * @return BigDecimal commission rate or ZERO if not set
   */
  public BigDecimal getCommissionRate() {
    return getDecimal(ProductMetadataKey.COMMISSION_RATE);
  }

  /**
   * Sets the maturity date.
   *
   * @param maturityDate The maturity date
   */
  public void putMaturityDate(final LocalDate maturityDate) {
    put(ProductMetadataKey.MATURITY_DATE, maturityDate);
  }

  private void put(final ProductMetadataKey key, final Object value) {
    if (value instanceof BigDecimal) {
      data.put(key, JbhMoneyUtils.withJBHDecimals((BigDecimal) value));
      return;
    }
    data.put(key, value);
  }

  /**
   * Sets the opening date.
   *
   * @param openingDate The opening date
   */
  public void putOpeningDate(final LocalDate openingDate) {
    put(ProductMetadataKey.OPENING_DATE, openingDate);
  }

  /**
   * Sets the term length in days.
   *
   * @param termLengthInDays The term length in days
   */
  public void putTermLengthInDays(final Integer termLengthInDays) {
    put(ProductMetadataKey.TERM_LENGTH_IN_DAYS, termLengthInDays);
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

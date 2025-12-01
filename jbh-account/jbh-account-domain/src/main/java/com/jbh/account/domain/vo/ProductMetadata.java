package com.jbh.account.domain.vo;

import com.jbh.account.domain.utils.JbhMoneyUtils;
import com.jbh.account.domain.vo.metadata.CdtMetadata;
import com.jbh.account.domain.vo.metadata.CommonMetadata;
import com.jbh.account.domain.vo.metadata.CreditCardMetadata;
import com.jbh.account.domain.vo.metadata.InvestmentMetadata;
import com.jbh.account.domain.vo.metadata.LoanMetadata;
import com.jbh.account.domain.vo.metadata.RealEstateMetadata;
import java.math.BigDecimal;
import java.util.EnumMap;
import java.util.Map;

/**
 * Value Object that encapsulates product metadata and provides type-safe access to metadata
 * fields. This VO can be used across all layers (domain, application, infrastructure) in the
 * hexagonal architecture without breaking layer isolation.
 *
 * <p>This class uses composition to organize metadata by product type, reducing the number of
 * public methods and improving cohesion. Access type-specific metadata through fluent accessors:
 *
 * <ul>
 *   <li>metadata.getCommon() - for common metadata across all product types
 *   <li>metadata.getRealEstate() - for real estate investment metadata
 *   <li>metadata.getLoan() - for loan metadata
 *   <li>metadata.getCreditCard() - for credit card metadata
 *   <li>metadata.getInvestment() - for investment metadata
 *   <li>metadata.getCdt() - for CDT metadata
 * </ul>
 */
public final class ProductMetadata {

  private final Map<ProductMetadataKey, Object> data;

  // Lazy-initialized type-specific metadata
  private CommonMetadata common;
  private RealEstateMetadata realEstate;
  private LoanMetadata loan;
  private CreditCardMetadata creditCard;
  private InvestmentMetadata investment;
  private CdtMetadata cdt;

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
   * Creates an empty ProductMetadata instance.
   *
   * @return ProductMetadata with no data
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
  public static ProductMetadata fromMap(final Map<ProductMetadataKey, Object> data) {
    return new ProductMetadata(data);
  }

  /**
   * Returns a defensive copy of the underlying metadata map. Used for persistence and
   * serialization.
   *
   * @return A new EnumMap containing all metadata entries
   */
  public Map<ProductMetadataKey, Object> asMap() {
    return new EnumMap<>(data);
  }

  /**
   * Gets common metadata shared across all product types.
   *
   * @return CommonMetadata instance
   */
  public CommonMetadata getCommon() {
    if (common == null) {
      common = new CommonMetadata(data);
    }
    return common;
  }

  /**
   * Gets real estate investment specific metadata.
   *
   * @return RealEstateMetadata instance
   */
  public RealEstateMetadata getRealEstate() {
    if (realEstate == null) {
      realEstate = new RealEstateMetadata(data);
    }
    return realEstate;
  }

  /**
   * Gets loan specific metadata.
   *
   * @return LoanMetadata instance
   */
  public LoanMetadata getLoan() {
    if (loan == null) {
      loan = new LoanMetadata(data);
    }
    return loan;
  }

  /**
   * Gets credit card specific metadata.
   *
   * @return CreditCardMetadata instance
   */
  public CreditCardMetadata getCreditCard() {
    if (creditCard == null) {
      creditCard = new CreditCardMetadata(data);
    }
    return creditCard;
  }

  /**
   * Gets investment specific metadata.
   *
   * @return InvestmentMetadata instance
   */
  public InvestmentMetadata getInvestment() {
    if (investment == null) {
      investment = new InvestmentMetadata(data);
    }
    return investment;
  }

  /**
   * Gets CDT (Certificate of Deposit) specific metadata.
   *
   * @return CdtMetadata instance
   */
  public CdtMetadata getCdt() {
    if (cdt == null) {
      cdt = new CdtMetadata(data);
    }
    return cdt;
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

  /**
   * Returns a defensive copy of the underlying metadata map.
   *
   * @return A new EnumMap containing all metadata entries
   */
  public Map<ProductMetadataKey, Object> getData() {
    return new EnumMap<>(data);
  }
}

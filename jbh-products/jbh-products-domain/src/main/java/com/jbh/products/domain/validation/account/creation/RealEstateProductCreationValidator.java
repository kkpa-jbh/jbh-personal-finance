package com.jbh.products.domain.validation.account.creation;

import static com.jbh.products.domain.vo.ProductMetadataKey.REAL_ESTATE_DOWN_PAYMENT_AMOUNT;

import com.jbh.commons.exception.BusinessException;
import com.jbh.commons.util.JbhMoneyUtils;
import com.jbh.products.domain.entity.MovementDomain;
import com.jbh.products.domain.entity.ProductDomain;
import com.jbh.products.domain.exceptions.BusinessDomainExceptionType;
import com.jbh.products.domain.vo.ProductMetadata;
import com.jbh.products.domain.vo.ProductMetadataKey;
import com.jbh.products.domain.vo.ProductType;
import java.math.BigDecimal;
import java.util.List;

@SuppressWarnings("PMD.LawOfDemeter")
public class RealEstateProductCreationValidator extends BaseAccountCreationValidator
    implements ProductCreationValidator {

  @Override
  public void validateMetadata(final ProductMetadata metadata) throws BusinessException {
    final List<ProductMetadataKey> requiredMetadata =
        ProductMetadataKey.findRequiredMetadataBy(ProductType.REAL_ESTATE_INVESTMENT);

    // Validate required metadata
    for (final ProductMetadataKey key : requiredMetadata) {
      if (!metadata.hasKey(key)) {
        throw new BusinessException(BusinessDomainExceptionType.MISSING_METADATA, key.name());
      }
    }

    // Validate Percentage
    final var realEstate = metadata.findRealEstateMetadata();
    JbhMoneyUtils.validatePercentage(realEstate.getDownPaymentPercentage());

    if (!metadata.hasKey(REAL_ESTATE_DOWN_PAYMENT_AMOUNT)) {
      final BigDecimal purchasePrice = realEstate.getPurchasePrice();
      final BigDecimal downPaymentPercentage = realEstate.getDownPaymentPercentage();
      final BigDecimal downPaymentAmount = purchasePrice.multiply(downPaymentPercentage);
      realEstate.putDownPaymentAmount(JbhMoneyUtils.withJBHDecimals(downPaymentAmount));
    }
  }

  @Override
  public void validateInsufficientNetFlow(
      final ProductDomain productDomain, final MovementDomain movement)
      throws BusinessException {
    // No validation needed for real estate products
  }
}

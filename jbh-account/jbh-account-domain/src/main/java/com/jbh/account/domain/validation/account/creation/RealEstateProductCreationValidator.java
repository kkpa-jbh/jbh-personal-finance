package com.jbh.account.domain.validation.account.creation;

import static com.jbh.account.domain.vo.ProductMetadataKey.REAL_ESTATE_DOWN_PAYMENT_AMOUNT;

import com.jbh.account.domain.entity.AccountMovementDomain;
import com.jbh.account.domain.entity.ProductDomain;
import com.jbh.account.domain.exceptions.ProductBusinessException;
import com.jbh.account.domain.exceptions.BusinessDomainExceptionType;
import com.jbh.account.domain.utils.JbhMoneyUtils;
import com.jbh.account.domain.vo.ProductMetadata;
import com.jbh.account.domain.vo.ProductMetadataKey;
import com.jbh.account.domain.vo.ProductType;
import java.math.BigDecimal;
import java.util.List;

@SuppressWarnings("PMD.LawOfDemeter")
public class RealEstateProductCreationValidator extends BaseAccountCreationValidator
    implements ProductCreationValidator {

  @Override
  public void validateMetadata(final ProductMetadata metadata) throws ProductBusinessException {
    final List<ProductMetadataKey> requiredMetadata =
        ProductMetadataKey.findRequiredMetadataBy(ProductType.REAL_ESTATE_INVESTMENT);

    // Validate required metadata
    for (final ProductMetadataKey key : requiredMetadata) {
      if (!metadata.hasKey(key)) {
        throw new ProductBusinessException(
            BusinessDomainExceptionType.MISSING_METADATA, key.name());
      }
    }

    // Validate Percentage
    final var realEstate = metadata.getRealEstate();
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
      final ProductDomain productDomain, final AccountMovementDomain movement)
      throws ProductBusinessException {
    // No validation needed for real estate products
  }
}

package com.jbh.finance.application.feature.movement.commands;

import static com.jbh.commons.util.JbhMoneyUtils.isZero;

import com.jbh.commons.exception.GenericSpecificationException;
import com.jbh.finance.application.feature.movement.dto.ExternalProductInfoDTO;
import com.jbh.finance.application.shared.validation.CommandValidator;
import com.jbh.finance.domain.product.vo.ProductPK;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;

// Tax and Fees Handling
// TaxWithholdingStrategy taxStrategy,  // How to handle tax implications

/**
 * @param toInternalAccount
 * @param toExternalAccount
 * @param currentBalance The latest current balance of the cdt/investment product
 * @param liquidatedDate
 */
public record LiquidateProductCommand(
    Optional<ProductPK> toInternalAccount,
    Optional<ExternalProductInfoDTO> toExternalAccount,
    BigDecimal currentBalance,
    LocalDate liquidatedDate)
    implements CommandValidator {

  public LiquidateProductCommand {
    validateAccounts(toInternalAccount, toExternalAccount);
    validateTotalAmount(currentBalance);
    validateTransferDate(liquidatedDate);
  }

  private static void validateAccounts(
      final Optional<ProductPK> toInternalAccount,
      final Optional<ExternalProductInfoDTO> toExternalAccount) {
    if (toInternalAccount.isEmpty() && toExternalAccount.isEmpty()) {
      throw new GenericSpecificationException(
          "Either toInternalAccount or toExternalAccount must be provided");
    }
  }

  private static void validateTotalAmount(final BigDecimal totalAmount) {
    if (totalAmount == null || isZero(totalAmount)) {
      throw new GenericSpecificationException("Total amount cannot be null or zero");
    }
  }

  private void validateTransferDate(final LocalDate transferDate) {
    if (transferDate == null) {
      throw new GenericSpecificationException("Transfer date cannot be null");
    }
  }

  @Override
  public void validate() {
    validateAccounts(toInternalAccount, toExternalAccount);
    validateTotalAmount(currentBalance);
    validateTransferDate(liquidatedDate);
  }
}

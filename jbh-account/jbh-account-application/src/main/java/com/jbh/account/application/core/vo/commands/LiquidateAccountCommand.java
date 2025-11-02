package com.jbh.account.application.core.vo.commands;

import static com.jbh.account.domain.utils.JbhMoneyUtils.isZero;

import com.jbh.account.domain.exceptions.GenericSpecificationException;
import com.jbh.account.domain.vo.AccountPK;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;

// Tax and Fees Handling
// TaxWithholdingStrategy taxStrategy,  // How to handle tax implications

public record LiquidateAccountCommand(
    Optional<AccountPK> toInternalAccount,
    Optional<ExternalAccountInfoVO> toExternalAccount,
    BigDecimal totalAmount,
    LocalDate transferDate)
    implements CommandValidator {

  public LiquidateAccountCommand {
    validateAccounts(toInternalAccount, toExternalAccount);
    validateTotalAmount(totalAmount);
    validateTransferDate(transferDate);
  }

  private static void validateAccounts(
      final Optional<AccountPK> toInternalAccount,
      final Optional<ExternalAccountInfoVO> toExternalAccount) {
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
    validateTotalAmount(totalAmount);
    validateTransferDate(transferDate);
  }
}

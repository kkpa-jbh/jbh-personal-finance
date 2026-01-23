package com.jbh.account.application.core.vo.commands;

import com.jbh.account.application.core.exceptions.BusinessApplicationExceptionType;
import com.jbh.account.domain.exceptions.ProductBusinessException;
import com.jbh.account.domain.utils.JbhMoneyUtils;
import com.jbh.account.domain.vo.ProductPK;
import java.math.BigDecimal;
import java.time.LocalDate;

@SuppressWarnings("PMD.CyclomaticComplexity")
public record AddTransferCommand(
    ProductPK toAccount, BigDecimal totalAmount, LocalDate transferDate) {

  public void validate() throws ProductBusinessException {
    if (toAccount == null || toAccount.userId() == null || toAccount.accountId() == null) {
      throw new ProductBusinessException(
          BusinessApplicationExceptionType.INVALID_TRANSFER_RECIPIENT);
    }

    if (JbhMoneyUtils.isZero(totalAmount) || JbhMoneyUtils.isNegative(totalAmount)) {
      throw new ProductBusinessException(BusinessApplicationExceptionType.INVALID_TRANSFER_AMOUNT);
    }

    if (transferDate == null) {
      throw new ProductBusinessException(BusinessApplicationExceptionType.INVALID_TRANSFER_DATE);
    }
  }
}

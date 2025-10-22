package com.jbh.account.application.core.vo.commands;

import com.jbh.account.application.core.exceptions.BusinessApplicationExceptionType;
import com.jbh.account.domain.exceptions.AccountBusinessException;
import com.jbh.account.domain.utils.JbhMoneyUtils;
import com.jbh.account.domain.vo.AccountPK;
import java.math.BigDecimal;
import java.time.LocalDate;

@SuppressWarnings("PMD.CyclomaticComplexity")
public record AddTransferCommand(
    AccountPK toAccount, BigDecimal totalAmount, LocalDate transferDate) {

  public void validate() throws AccountBusinessException {
    if (toAccount == null || toAccount.userId() == null || toAccount.accountId() == null) {
      throw new AccountBusinessException(
          BusinessApplicationExceptionType.INVALID_TRANSFER_RECIPIENT);
    }

    if (JbhMoneyUtils.isZero(totalAmount) || JbhMoneyUtils.isNegative(totalAmount)) {
      throw new AccountBusinessException(BusinessApplicationExceptionType.INVALID_TRANSFER_AMOUNT);
    }

    if (transferDate == null) {
      throw new AccountBusinessException(BusinessApplicationExceptionType.INVALID_TRANSFER_DATE);
    }
  }
}

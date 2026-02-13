package com.jbh.finance.application.feature.movement.commands;

import com.jbh.finance.application.shared.exceptions.BusinessApplicationExceptionType;
import com.jbh.finance.domain.product.vo.ProductPK;
import com.jbh.commons.exception.BusinessException;
import com.jbh.commons.util.JbhMoneyUtils;
import java.math.BigDecimal;
import java.time.LocalDate;

@SuppressWarnings("PMD.CyclomaticComplexity")
public record AddTransferCommand(
    ProductPK toAccount, BigDecimal totalAmount, LocalDate transferDate) {

  public void validate() throws BusinessException {
    if (toAccount == null || toAccount.userId() == null || toAccount.productId() == null) {
      throw new BusinessException(BusinessApplicationExceptionType.INVALID_TRANSFER_RECIPIENT);
    }

    if (JbhMoneyUtils.isZero(totalAmount) || JbhMoneyUtils.isNegative(totalAmount)) {
      throw new BusinessException(BusinessApplicationExceptionType.INVALID_TRANSFER_AMOUNT);
    }

    if (transferDate == null) {
      throw new BusinessException(BusinessApplicationExceptionType.INVALID_TRANSFER_DATE);
    }
  }
}

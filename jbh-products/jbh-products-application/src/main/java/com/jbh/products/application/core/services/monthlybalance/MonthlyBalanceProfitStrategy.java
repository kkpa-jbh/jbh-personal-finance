package com.jbh.products.application.core.services.monthlybalance;

import com.jbh.products.application.core.dto.MonthlyBalanceDTO;
import com.jbh.products.application.core.vo.commands.AddMonthlyBalanceCommand;
import com.jbh.products.domain.product.vo.ProductPK;
import com.jbh.commons.exception.BusinessException;

public interface MonthlyBalanceProfitStrategy {

  MonthlyBalanceDTO registerOfficialMonthlyBalance(
      ProductPK accountPK, MonthlyBalanceDTO monthlyBalanceDomain, AddMonthlyBalanceCommand command)
      throws BusinessException;
}

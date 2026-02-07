package com.jbh.products.application.feature.monthlybalance.services;

import com.jbh.products.application.feature.monthlybalance.dto.MonthlyBalanceDTO;
import com.jbh.products.application.feature.monthlybalance.commands.AddMonthlyBalanceCommand;
import com.jbh.products.domain.product.vo.ProductPK;
import com.jbh.commons.exception.BusinessException;

public interface MonthlyBalanceProfitStrategy {

  MonthlyBalanceDTO registerOfficialMonthlyBalance(
      ProductPK accountPK, MonthlyBalanceDTO monthlyBalanceDomain, AddMonthlyBalanceCommand command)
      throws BusinessException;
}

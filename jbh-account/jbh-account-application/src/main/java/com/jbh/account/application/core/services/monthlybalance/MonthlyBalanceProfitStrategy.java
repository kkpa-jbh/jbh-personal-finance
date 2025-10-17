package com.jbh.account.application.core.services.monthlybalance;

import com.jbh.account.application.core.dto.MonthlyBalanceDTO;
import com.jbh.account.application.core.vo.commands.AddMonthlyBalanceCommand;
import com.jbh.account.domain.exceptions.AccountBusinessException;
import com.jbh.account.domain.vo.AccountPK;

public interface MonthlyBalanceProfitStrategy {

  MonthlyBalanceDTO registerOfficialMonthlyBalance(
      AccountPK accountPK, MonthlyBalanceDTO monthlyBalanceDomain, AddMonthlyBalanceCommand command)
      throws AccountBusinessException;
}

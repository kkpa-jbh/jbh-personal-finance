package com.jbh.account.application.core.ports.output.monthlybalance;

import com.jbh.account.application.core.dto.MonthlyBalanceDTO;
import java.util.List;

public interface AccountMonthlyBalanceWriterRepository {
  void saveBalance(MonthlyBalanceDTO accountMonthlyBalance);

  List<MonthlyBalanceDTO> saveMultiBalances(List<MonthlyBalanceDTO> accountMonthlyBalance);
}

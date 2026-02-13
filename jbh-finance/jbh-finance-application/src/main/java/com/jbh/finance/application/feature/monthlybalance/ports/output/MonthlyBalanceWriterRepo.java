package com.jbh.finance.application.feature.monthlybalance.ports.output;

import com.jbh.finance.application.feature.monthlybalance.dto.MonthlyBalanceDTO;
import java.util.List;

public interface MonthlyBalanceWriterRepo {
  void saveBalance(MonthlyBalanceDTO accountMonthlyBalance);

  List<MonthlyBalanceDTO> saveMultiBalances(List<MonthlyBalanceDTO> accountMonthlyBalance);
}

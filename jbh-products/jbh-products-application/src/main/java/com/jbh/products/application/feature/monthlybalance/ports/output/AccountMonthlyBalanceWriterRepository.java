package com.jbh.products.application.feature.monthlybalance.ports.output;

import com.jbh.products.application.feature.monthlybalance.dto.MonthlyBalanceDTO;
import java.util.List;

public interface AccountMonthlyBalanceWriterRepository {
  void saveBalance(MonthlyBalanceDTO accountMonthlyBalance);

  List<MonthlyBalanceDTO> saveMultiBalances(List<MonthlyBalanceDTO> accountMonthlyBalance);
}

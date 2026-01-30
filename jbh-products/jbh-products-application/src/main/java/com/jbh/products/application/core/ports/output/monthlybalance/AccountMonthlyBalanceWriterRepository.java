package com.jbh.products.application.core.ports.output.monthlybalance;

import com.jbh.products.application.core.dto.MonthlyBalanceDTO;
import java.util.List;

public interface AccountMonthlyBalanceWriterRepository {
  void saveBalance(MonthlyBalanceDTO accountMonthlyBalance);

  List<MonthlyBalanceDTO> saveMultiBalances(List<MonthlyBalanceDTO> accountMonthlyBalance);
}

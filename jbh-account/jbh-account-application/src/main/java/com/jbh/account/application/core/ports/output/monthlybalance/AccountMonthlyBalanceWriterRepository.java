package com.jbh.account.application.core.ports.output.monthlybalance;

import com.jbh.account.application.core.dto.AccountMonthlyBalanceDTO;
import java.util.List;

public interface AccountMonthlyBalanceWriterRepository {
  void saveBalance(AccountMonthlyBalanceDTO accountMonthlyBalance);

  List<AccountMonthlyBalanceDTO> saveMultiBalances(
      List<AccountMonthlyBalanceDTO> accountMonthlyBalance);
}

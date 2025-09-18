package com.jbh.account.application.accounts.ports.output.monthlybalance;

import com.jbh.account.domain.vo.AccountMonthlyBalanceDTO;
import java.util.List;

public interface AccountMonthlyBalanceWriterRepository {
  void saveBalance(AccountMonthlyBalanceDTO accountMonthlyBalance);

  List<AccountMonthlyBalanceDTO> saveMultiBalances(
      List<AccountMonthlyBalanceDTO> accountMonthlyBalance);
}

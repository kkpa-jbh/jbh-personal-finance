package com.jbh.account.application.core.usecases;

import com.jbh.account.application.core.dto.MonthlyBalanceDTO;
import com.jbh.account.domain.exceptions.AccountBusinessException;
import com.jbh.account.domain.vo.AccountPK;
import java.time.YearMonth;
import java.util.List;

public interface FindMonthlyBalanceUseCase {

  List<MonthlyBalanceDTO> findByAccountAndPeriods(
      AccountPK accountPK, YearMonth startPeriod, YearMonth endPeriod)
      throws AccountBusinessException;
}

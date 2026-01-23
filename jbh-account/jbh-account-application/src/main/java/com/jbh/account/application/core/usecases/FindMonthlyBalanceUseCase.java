package com.jbh.account.application.core.usecases;

import com.jbh.account.application.core.dto.MonthlyBalanceDTO;
import com.jbh.account.domain.exceptions.ProductBusinessException;
import com.jbh.account.domain.vo.ProductPK;
import java.time.YearMonth;
import java.util.List;

public interface FindMonthlyBalanceUseCase {

  List<MonthlyBalanceDTO> findByAccountAndPeriods(
      ProductPK accountPK, YearMonth startPeriod, YearMonth endPeriod)
      throws ProductBusinessException;
}

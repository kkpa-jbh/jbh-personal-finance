package com.jbh.products.application.core.usecases;

import com.jbh.products.application.core.dto.MonthlyBalanceDTO;
import com.jbh.products.domain.vo.ProductPK;
import com.jbh.commons.exception.BusinessException;
import java.time.YearMonth;
import java.util.List;

public interface FindMonthlyBalanceUseCase {

  List<MonthlyBalanceDTO> findByAccountAndPeriods(
      ProductPK accountPK, YearMonth startPeriod, YearMonth endPeriod) throws BusinessException;
}

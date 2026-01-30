package com.jbh.products.application.core.usecases;

import com.jbh.products.application.core.dto.MonthlyBalanceDTO;
import com.jbh.products.application.core.vo.commands.AddMonthlyBalanceCommand;
import com.jbh.products.domain.vo.ProductId;
import com.jbh.commons.exception.BusinessException;
import java.time.LocalDate;
import java.util.UUID;

public interface RegisterMonthlyBalanceUseCase {

  MonthlyBalanceDTO registerOfficialMonthlyBalance(
      LocalDate runningDate, UUID userId, ProductId accountId, AddMonthlyBalanceCommand command)
      throws BusinessException;
}

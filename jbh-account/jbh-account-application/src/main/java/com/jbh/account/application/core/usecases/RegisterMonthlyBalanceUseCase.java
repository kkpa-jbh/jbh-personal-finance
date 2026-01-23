package com.jbh.account.application.core.usecases;

import com.jbh.account.application.core.dto.MonthlyBalanceDTO;
import com.jbh.account.application.core.vo.commands.AddMonthlyBalanceCommand;
import com.jbh.account.domain.exceptions.ProductBusinessException;
import com.jbh.account.domain.vo.ProductId;
import java.time.LocalDate;
import java.util.UUID;

public interface RegisterMonthlyBalanceUseCase {

  MonthlyBalanceDTO registerOfficialMonthlyBalance(
      LocalDate runningDate, UUID userId, ProductId accountId, AddMonthlyBalanceCommand command)
      throws ProductBusinessException;
}

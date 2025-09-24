package com.jbh.account.application.core.usecases;

import com.jbh.account.application.core.dto.MonthlyBalanceDTO;
import com.jbh.account.application.core.vo.commands.AddMonthlyBalanceCommand;
import com.jbh.account.application.exceptions.JbhSpecificationApplication;
import com.jbh.account.domain.vo.AccountId;
import java.time.LocalDate;
import java.util.UUID;

public interface RegisterMonthlyBalanceUseCase {

  MonthlyBalanceDTO registerOfficialMonthlyBalance(
      LocalDate runningDate, UUID userId, AccountId accountId, AddMonthlyBalanceCommand command)
      throws JbhSpecificationApplication;
}

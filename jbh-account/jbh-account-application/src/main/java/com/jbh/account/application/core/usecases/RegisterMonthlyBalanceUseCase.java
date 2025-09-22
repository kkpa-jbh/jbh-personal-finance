package com.jbh.account.application.core.usecases;

import com.jbh.account.application.core.dto.AccountMonthlyBalanceDTO;
import com.jbh.account.application.core.vo.commands.AddMonthlyBalanceCommand;
import com.jbh.account.application.exceptions.JbhSpecificationApplication;
import com.jbh.account.domain.vo.AccountId;
import java.time.LocalDate;
import java.util.UUID;

public interface RegisterMonthlyBalanceUseCase {

  AccountMonthlyBalanceDTO registerOfficialMonthlyBalance(
      LocalDate runningDate, UUID userId, AccountId accountId, AddMonthlyBalanceCommand command)
      throws JbhSpecificationApplication;
}

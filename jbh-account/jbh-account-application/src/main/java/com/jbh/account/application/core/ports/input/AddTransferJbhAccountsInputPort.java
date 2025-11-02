package com.jbh.account.application.core.ports.input;

import com.jbh.account.application.core.dto.AccountDTO;
import com.jbh.account.application.core.services.account.AccountService;
import com.jbh.account.application.core.services.movements.AccountMovementApplicationService;
import com.jbh.account.application.core.usecases.AddTransferJbhAccountsUseCase;
import com.jbh.account.application.core.vo.commands.AddMovementCommand;
import com.jbh.account.application.core.vo.commands.AddTransferCommand;
import com.jbh.account.domain.exceptions.AccountBusinessException;
import com.jbh.account.domain.vo.AccountPK;
import com.jbh.account.domain.vo.ExpenseCategory;
import com.jbh.account.domain.vo.IncomeCategory;
import com.jbh.account.domain.vo.MovementCategoryDTO;
import com.jbh.account.domain.vo.MovementType;
import java.math.BigDecimal;
import java.time.LocalDate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AddTransferJbhAccountsInputPort implements AddTransferJbhAccountsUseCase {

  private static final Logger LOG = LoggerFactory.getLogger(AddTransferJbhAccountsInputPort.class);
  private final AccountMovementApplicationService accountMovementService;
  private final AccountService accountService;

  public AddTransferJbhAccountsInputPort(
      final AccountService accountService,
      final AccountMovementApplicationService accountMovementService) {

    this.accountService = accountService;
    this.accountMovementService = accountMovementService;
  }

  @Override
  public void addTransfer(final AccountPK fromAccount, final AddTransferCommand transferCommand)
      throws AccountBusinessException {

    transferCommand.validate();

    final LocalDate transferDate = transferCommand.transferDate();
    final BigDecimal transferAmount = transferCommand.totalAmount();

    final AccountDTO fromAccountDTO = accountService.findAccountOrThrow(fromAccount.accountId());
    final AccountDTO toAccountDTO =
        accountService.findAccountOrThrow(transferCommand.toAccount().accountId());

    final var fromAccountName = fromAccountDTO.name();
    final var toAccountName = toAccountDTO.name();

    LOG.info(
        "Sending transfer from {} to {} for {}",
        fromAccountName,
        toAccountDTO.name(),
        transferAmount);

    LOG.info("Registering the withdrawal movement for the account {}", fromAccountName);
    accountMovementService.addMovementProcessingBalances(
        fromAccount,
        new AddMovementCommand(
            transferDate,
            transferAmount,
            MovementType.WITHDRAWAL,
            MovementCategoryDTO.withType(ExpenseCategory.TRANSFER)));

    LOG.info("Registering the deposit movement for the account {}", toAccountName);
    accountMovementService.addMovementProcessingBalances(
        new AccountPK(transferCommand.toAccount().userId(), toAccountDTO.id()),
        new AddMovementCommand(
            transferDate,
            transferAmount,
            MovementType.DEPOSIT,
            MovementCategoryDTO.withType(IncomeCategory.TRANSFER)));
  }
}

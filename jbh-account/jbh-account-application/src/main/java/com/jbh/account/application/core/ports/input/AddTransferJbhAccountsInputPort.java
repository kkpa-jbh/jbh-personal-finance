package com.jbh.account.application.core.ports.input;

import com.jbh.account.application.core.dto.ProductDTO;
import com.jbh.account.application.core.mappers.AccountMapper;
import com.jbh.account.application.core.mappers.MovementMapper;
import com.jbh.account.application.core.services.account.AccountService;
import com.jbh.account.application.core.services.movements.AccountMovementApplicationService;
import com.jbh.account.application.core.usecases.AddTransferJbhAccountsUseCase;
import com.jbh.account.application.core.vo.commands.AddMovementCommand;
import com.jbh.account.application.core.vo.commands.AddTransferCommand;
import com.jbh.account.domain.exceptions.AccountBusinessException;
import com.jbh.account.domain.exceptions.BusinessDomainExceptionType;
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

    final ProductDTO fromAccountDTO = accountService.findAccountOrThrow(fromAccount.accountId());
    final ProductDTO toAccountDTO =
        accountService.findAccountOrThrow(transferCommand.toAccount().accountId());

    final var fromAccountName = fromAccountDTO.name();
    final var toAccountName = toAccountDTO.name();

    LOG.info(
        "Sending transfer from {} to {} for {}",
        fromAccountName,
        toAccountDTO.name(),
        transferAmount);

    final var movementCommandFrom =
        new AddMovementCommand(
            transferDate,
            transferAmount,
            MovementType.DEPOSIT,
            MovementCategoryDTO.withType(IncomeCategory.TRANSFER));
    transferValidationFROM(fromAccountDTO, movementCommandFrom);

    final var movementCommandTo =
        new AddMovementCommand(
            transferDate,
            transferAmount,
            MovementType.WITHDRAWAL,
            MovementCategoryDTO.withType(ExpenseCategory.TRANSFER));
    transferValidationTO(toAccountDTO, movementCommandTo);

    LOG.info("Registering the deposit movement for the account {}", toAccountName);
    accountMovementService.addMovementProcessingBalances(
        new AccountPK(toAccountDTO.userId(), toAccountDTO.id()), movementCommandFrom);

    LOG.info("Registering the withdrawal movement for the account {}", fromAccountName);
    accountMovementService.addMovementProcessingBalances(fromAccount, movementCommandTo);
  }

  /**
   * Validates if the 'from' account has sufficient net flow to perform the transfer.
   *
   * @param fromProductDTO the 'from' account
   * @param movementCommand the movement command representing the transfer
   * @throws AccountBusinessException if the 'from' account does not have sufficient net flow
   */
  private void transferValidationFROM(
      final ProductDTO fromProductDTO, final AddMovementCommand movementCommand)
      throws AccountBusinessException {
    final var movementDTO = MovementMapper.fromCommand(fromProductDTO.id(), movementCommand);
    AccountMapper.toDomain(fromProductDTO)
        .validateInsufficientNetFlow(MovementMapper.toDomain(movementDTO));
  }

  private void transferValidationTO(
      final ProductDTO toAccountDTO, final AddMovementCommand movementCommandTo)
      throws AccountBusinessException {
    if (toAccountDTO.isLoan()) {
      final BigDecimal movementAmount = movementCommandTo.totalAmount();
      final BigDecimal pendingToPaid = toAccountDTO.metadata().getLoanPayoffAmountToday();

      if (movementAmount.compareTo(pendingToPaid) > 0) {
        throw new AccountBusinessException(
            BusinessDomainExceptionType.PAYMENT_AMOUNT_GREATER_PAYOFF);
      }
    }
  }
}

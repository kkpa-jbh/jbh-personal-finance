package com.jbh.products.application.feature.movement.ports.input;

import com.jbh.commons.exception.BusinessException;
import com.jbh.products.application.feature.movement.commands.AddMovementCommand;
import com.jbh.products.application.feature.movement.commands.AddTransferCommand;
import com.jbh.products.application.feature.movement.mappers.MovementMapper;
import com.jbh.products.application.feature.movement.services.AccountMovementApplicationService;
import com.jbh.products.application.feature.movement.usecases.AddTransferJbhAccountsUseCase;
import com.jbh.products.application.feature.product.dto.ProductDTO;
import com.jbh.products.application.feature.product.mappers.ProductMapper;
import com.jbh.products.application.feature.product.services.ProductsService;
import com.jbh.products.domain.movement.vo.ExpenseCategory;
import com.jbh.products.domain.movement.vo.IncomeCategory;
import com.jbh.products.domain.movement.vo.MovementCategoryVO;
import com.jbh.products.domain.movement.vo.MovementType;
import com.jbh.products.domain.product.vo.ProductPK;
import com.jbh.products.domain.shared.exceptions.BusinessDomainExceptionType;
import java.math.BigDecimal;
import java.time.LocalDate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AddTransferJbhAccountsInputPort implements AddTransferJbhAccountsUseCase {

  private static final Logger LOG = LoggerFactory.getLogger(AddTransferJbhAccountsInputPort.class);
  private final AccountMovementApplicationService accountMovementService;
  private final ProductsService accountService;

  public AddTransferJbhAccountsInputPort(
      final ProductsService accountService,
      final AccountMovementApplicationService accountMovementService) {

    this.accountService = accountService;
    this.accountMovementService = accountMovementService;
  }

  @Override
  public void addTransfer(final ProductPK fromAccount, final AddTransferCommand transferCommand)
      throws BusinessException {

    transferCommand.validate();

    final LocalDate transferDate = transferCommand.transferDate();
    final BigDecimal transferAmount = transferCommand.totalAmount();

    final ProductDTO fromAccountDTO = accountService.findProductOrThrow(fromAccount.accountId());
    final ProductDTO toAccountDTO =
        accountService.findProductOrThrow(transferCommand.toAccount().accountId());

    final var fromAccountName = fromAccountDTO.name();
    final var toAccountName = toAccountDTO.name();

    LOG.info(
        "Sending transfer from {} to {} for {}",
        fromAccountName,
        toAccountDTO.name(),
        transferAmount);

    final MovementCategoryVO incomeTransferCategory =
        MovementCategoryVO.withType(IncomeCategory.TRANSFER);
    final MovementType movementType = MovementType.findByCategory(incomeTransferCategory);

    final var movementCommandFrom =
        new AddMovementCommand(
            transferDate,
            transferAmount,
            null,
            movementType,
            incomeTransferCategory,
            "Transfer from " + fromAccountName + " to " + toAccountName);
    transferValidationFROM(fromAccountDTO, movementCommandFrom);

    final MovementCategoryVO expenseTransferCategory =
        MovementCategoryVO.withType(ExpenseCategory.TRANSFER);
    final MovementType expenseTransferMovementType =
        MovementType.findByCategory(expenseTransferCategory);
    final var movementCommandTo =
        new AddMovementCommand(
            transferDate,
            transferAmount,
            null,
            expenseTransferMovementType,
            expenseTransferCategory,
            "Transfer from " + fromAccountName + " to " + toAccountName);

    transferValidationTO(toAccountDTO, movementCommandTo);

    LOG.info("Registering the deposit movement for the account {}", toAccountName);
    accountMovementService.addMovementProcessingBalances(
        new ProductPK(toAccountDTO.userId(), toAccountDTO.id()), movementCommandFrom);

    LOG.info("Registering the withdrawal movement for the account {}", fromAccountName);
    accountMovementService.addMovementProcessingBalances(fromAccount, movementCommandTo);
  }

  /**
   * Validates if the 'from' account has sufficient net flow to perform the transfer.
   *
   * @param fromProductDTO the 'from' account
   * @param movementCommand the movement command representing the transfer
   * @throws BusinessException if the 'from' account does not have sufficient net flow
   */
  private void transferValidationFROM(
      final ProductDTO fromProductDTO, final AddMovementCommand movementCommand)
      throws BusinessException {
    final var movementDTO = MovementMapper.fromCommand(fromProductDTO.id(), movementCommand);
    ProductMapper.toDomain(fromProductDTO)
        .validateInsufficientNetFlow(MovementMapper.toDomain(movementDTO));
  }

  private void transferValidationTO(
      final ProductDTO toAccountDTO, final AddMovementCommand movementCommandTo)
      throws BusinessException {
    if (toAccountDTO.isLoan()) {
      final BigDecimal movementAmount = movementCommandTo.totalAmount();
      final BigDecimal pendingToPaid =
          toAccountDTO.metadata().findLoanMetadata().getPayoffAmountToday();

      if (movementAmount.compareTo(pendingToPaid) > 0) {
        throw new BusinessException(BusinessDomainExceptionType.PAYMENT_AMOUNT_GREATER_PAYOFF);
      }
    }
  }
}

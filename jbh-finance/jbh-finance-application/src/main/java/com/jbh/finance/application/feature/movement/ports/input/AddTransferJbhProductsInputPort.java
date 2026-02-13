package com.jbh.finance.application.feature.movement.ports.input;

import com.jbh.commons.exception.BusinessException;
import com.jbh.finance.application.feature.movement.commands.AddMovementCommand;
import com.jbh.finance.application.feature.movement.commands.AddTransferCommand;
import com.jbh.finance.application.feature.movement.mappers.MovementMapper;
import com.jbh.finance.application.feature.movement.services.ProcessMovementService;
import com.jbh.finance.application.feature.movement.usecases.AddTransferJbhProductsUseCase;
import com.jbh.finance.application.feature.product.dto.ProductDTO;
import com.jbh.finance.application.feature.product.mappers.ProductMapper;
import com.jbh.finance.application.feature.product.services.ProductLifecycleService;
import com.jbh.finance.domain.movement.vo.ExpenseCategory;
import com.jbh.finance.domain.movement.vo.IncomeCategory;
import com.jbh.finance.domain.movement.vo.MovementCategoryVO;
import com.jbh.finance.domain.movement.vo.MovementType;
import com.jbh.finance.domain.product.vo.ProductPK;
import com.jbh.finance.domain.shared.exceptions.BusinessDomainExceptionType;
import java.math.BigDecimal;
import java.time.LocalDate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AddTransferJbhProductsInputPort implements AddTransferJbhProductsUseCase {

  private static final Logger LOG = LoggerFactory.getLogger(AddTransferJbhProductsInputPort.class);
  private final ProcessMovementService processMovementService;
  private final ProductLifecycleService productLifecycleService;

  public AddTransferJbhProductsInputPort(
      final ProductLifecycleService productLifecycleService,
      final ProcessMovementService processMovementService) {

    this.productLifecycleService = productLifecycleService;
    this.processMovementService = processMovementService;
  }

  @Override
  public void addTransfer(final ProductPK fromAccount, final AddTransferCommand transferCommand)
      throws BusinessException {

    transferCommand.validate();

    final LocalDate transferDate = transferCommand.transferDate();
    final BigDecimal transferAmount = transferCommand.totalAmount();

    final ProductDTO fromAccountDTO =
        productLifecycleService.findOrThrowByIdProductId(fromAccount.productId());
    final ProductDTO toAccountDTO =
        productLifecycleService.findOrThrowByIdProductId(transferCommand.toAccount().productId());

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

    LOG.info("Registering the deposit movement for the productDTO {}", toAccountName);
    processMovementService.addMovementProcessingBalances(
        new ProductPK(toAccountDTO.userId(), toAccountDTO.id()), movementCommandFrom);

    LOG.info("Registering the withdrawal movement for the productDTO {}", fromAccountName);
    processMovementService.addMovementProcessingBalances(fromAccount, movementCommandTo);
  }

  /**
   * Validates if the 'from' productDTO has sufficient net flow to perform the transfer.
   *
   * @param fromProductDTO the 'from' productDTO
   * @param movementCommand the movement command representing the transfer
   * @throws BusinessException if the 'from' productDTO does not have sufficient net flow
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

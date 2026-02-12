package com.jbh.finance.application.feature.movement.ports.input;

import com.jbh.commons.exception.BusinessException;
import com.jbh.finance.application.feature.movement.commands.LiquidateAccountCommand;
import com.jbh.finance.application.feature.movement.dto.AddBasicMovementDTO;
import com.jbh.finance.application.feature.movement.dto.LiquidationResultDTO;
import com.jbh.finance.application.feature.movement.mappers.MovementMapper;
import com.jbh.finance.application.feature.movement.services.MovementApplicationService;
import com.jbh.finance.application.feature.movement.usecases.LiquidateAccountUseCase;
import com.jbh.finance.application.feature.product.dto.ProductDTO;
import com.jbh.finance.application.feature.product.services.ProductsService;
import com.jbh.finance.domain.movement.vo.MovementMetadata;
import com.jbh.finance.domain.product.vo.ProductId;
import com.jbh.finance.domain.product.vo.ProductPK;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class LiquidateAccountInputPort implements LiquidateAccountUseCase {

  private static final Logger LOG = LoggerFactory.getLogger(LiquidateAccountInputPort.class);

  private final ProductsService accountService;
  private final MovementApplicationService movementApplicationService;

  public LiquidateAccountInputPort(
      final ProductsService accountService,
      final MovementApplicationService movementApplicationService) {
    this.accountService = accountService;
    this.movementApplicationService = movementApplicationService;
  }

  @Override
  public LiquidationResultDTO liquidateAccount(
      final UUID userId,
      final ProductId accountId,
      final LiquidateAccountCommand liquidationCommand)
      throws BusinessException {

    liquidationCommand.validate();

    final var movementDTO = MovementMapper.fromCommand(accountId, liquidationCommand);
    ProductDTO toInternalAccount = null;
    if (liquidationCommand.toInternalAccount().isPresent()) {
      final var internalAccountId = liquidationCommand.toInternalAccount().get().accountId();
      toInternalAccount = accountService.findProductOrThrow(internalAccountId);
      movementDTO.metadata().putTargetInternalAccount(toInternalAccount.toDomain());
    }

    final ProductPK accountPK = new ProductPK(userId, accountId);

    LOG.info("Liquidating account {} ", accountId);
    final AddBasicMovementDTO addedMovementDTO =
        movementApplicationService.processMovement(movementDTO, accountPK, false);

    final ProductDTO syncedAccountDTO = addedMovementDTO.account();
    depositToAccount(liquidationCommand, toInternalAccount, syncedAccountDTO);

    return new LiquidationResultDTO(true);
  }

  private void depositToAccount(
      final LiquidateAccountCommand liquidationCommand,
      final ProductDTO toInternalAccount,
      final ProductDTO syncedAccountDTO)
      throws BusinessException {
    if (toInternalAccount != null) {
      LOG.info("Deposit dividends to internal account {} ", toInternalAccount);
      final var internalAccountId = toInternalAccount.id();
      final var totalAmount = liquidationCommand.currentBalance();
      final var transferDate = liquidationCommand.liquidatedDate();

      final ProductPK accountPK = new ProductPK(toInternalAccount.userId(), internalAccountId);
      final MovementMetadata metadata = MovementMetadata.createEmpty();
      metadata.putInvestmentIncomeAccount(syncedAccountDTO.toDomain());
      movementApplicationService.addDividendsMovement(
          accountPK, transferDate, totalAmount, null, null, metadata);
    }
  }
}

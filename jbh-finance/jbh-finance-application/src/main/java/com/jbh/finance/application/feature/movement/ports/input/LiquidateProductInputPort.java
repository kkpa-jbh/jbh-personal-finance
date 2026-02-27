package com.jbh.finance.application.feature.movement.ports.input;

import com.jbh.commons.exception.BusinessException;
import com.jbh.finance.application.feature.category.services.CategoryService;
import com.jbh.finance.application.feature.movement.commands.LiquidateProductCommand;
import com.jbh.finance.application.feature.movement.dto.AddBasicMovementDTO;
import com.jbh.finance.application.feature.movement.dto.LiquidationResultDTO;
import com.jbh.finance.application.feature.movement.mappers.MovementMapper;
import com.jbh.finance.application.feature.movement.services.ProcessMovementService;
import com.jbh.finance.application.feature.movement.usecases.LiquidateProductUseCase;
import com.jbh.finance.application.feature.product.dto.ProductDTO;
import com.jbh.finance.application.feature.product.services.ProductLifecycleService;
import com.jbh.finance.domain.movement.vo.MovementMetadata;
import com.jbh.finance.domain.product.vo.ProductId;
import com.jbh.finance.domain.product.vo.ProductPK;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class LiquidateProductInputPort implements LiquidateProductUseCase {

  private static final Logger LOG = LoggerFactory.getLogger(LiquidateProductInputPort.class);

  private final ProductLifecycleService accountService;
  private final ProcessMovementService movementApplicationService;
  private final CategoryService categoryService;

  public LiquidateProductInputPort(
      final ProductLifecycleService accountService,
      final ProcessMovementService movementApplicationService,
      final CategoryService categoryService) {
    this.accountService = accountService;
    this.movementApplicationService = movementApplicationService;
    this.categoryService = categoryService;
  }

  @Override
  public LiquidationResultDTO liquidateAccount(
      final UUID userId,
      final ProductId productId,
      final LiquidateProductCommand liquidationCommand)
      throws BusinessException {

    liquidationCommand.validate();

    final var movementDTO =
        MovementMapper.fromCommand(
            productId, liquidationCommand, categoryService.findInvestmentToCloseIt());
    ProductDTO toInternalAccount = null;
    if (liquidationCommand.toInternalAccount().isPresent()) {
      final var internalAccountId = liquidationCommand.toInternalAccount().get().productId();
      toInternalAccount = accountService.findOrThrowByIdProductId(internalAccountId);
      movementDTO.metadata().putTargetInternalAccount(toInternalAccount.toDomain());
    }

    final ProductPK accountPK = new ProductPK(userId, productId);

    LOG.info("Liquidating productDTO {} ", productId);
    final AddBasicMovementDTO addedMovementDTO =
        movementApplicationService.processMovement(movementDTO, accountPK, false);

    final ProductDTO syncedAccountDTO = addedMovementDTO.productDTO();
    depositToAccount(liquidationCommand, toInternalAccount, syncedAccountDTO);

    return new LiquidationResultDTO(true);
  }

  private void depositToAccount(
      final LiquidateProductCommand liquidationCommand,
      final ProductDTO toInternalAccount,
      final ProductDTO syncedAccountDTO)
      throws BusinessException {
    if (toInternalAccount != null) {
      LOG.info("Deposit dividends to internal productDTO {} ", toInternalAccount);
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

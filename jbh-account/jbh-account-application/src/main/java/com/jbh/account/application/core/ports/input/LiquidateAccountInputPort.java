package com.jbh.account.application.core.ports.input;

import com.jbh.account.application.core.dto.AccountDTO;
import com.jbh.account.application.core.dto.AddBasicMovementDTO;
import com.jbh.account.application.core.dto.LiquidationResultDTO;
import com.jbh.account.application.core.mappers.MovementMapper;
import com.jbh.account.application.core.services.account.AccountService;
import com.jbh.account.application.core.services.movements.AccountMovementApplicationService;
import com.jbh.account.application.core.usecases.LiquidateAccountUseCase;
import com.jbh.account.application.core.vo.commands.LiquidateAccountCommand;
import com.jbh.account.domain.exceptions.AccountBusinessException;
import com.jbh.account.domain.vo.AccountId;
import com.jbh.account.domain.vo.AccountMovementMetadata;
import com.jbh.account.domain.vo.AccountPK;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class LiquidateAccountInputPort implements LiquidateAccountUseCase {

  private static final Logger LOG = LoggerFactory.getLogger(LiquidateAccountInputPort.class);

  private final AccountService accountService;
  private final AccountMovementApplicationService movementApplicationService;

  public LiquidateAccountInputPort(
      final AccountService accountService,
      final AccountMovementApplicationService movementApplicationService) {
    this.accountService = accountService;
    this.movementApplicationService = movementApplicationService;
  }

  @Override
  public LiquidationResultDTO liquidateAccount(
      final UUID userId,
      final AccountId accountId,
      final LiquidateAccountCommand liquidationCommand)
      throws AccountBusinessException {

    liquidationCommand.validate();

    final var movementDTO = MovementMapper.fromCommand(accountId, liquidationCommand);
    AccountDTO toInternalAccount = null;
    if (liquidationCommand.toInternalAccount().isPresent()) {
      final var internalAccountId = liquidationCommand.toInternalAccount().get().accountId();
      toInternalAccount = accountService.findAccountOrThrow(internalAccountId);
      movementDTO.metadata().putTargetInternalAccount(toInternalAccount.toDomain());
    }

    final AccountPK accountPK = new AccountPK(userId, accountId);

    LOG.info("Liquidating account {} ", accountId);
    final AddBasicMovementDTO addedMovementDTO =
        movementApplicationService.processMovement(movementDTO, accountPK, false);

    final AccountDTO syncedAccountDTO = addedMovementDTO.account();
    depositToAccount(liquidationCommand, toInternalAccount, syncedAccountDTO);

    return new LiquidationResultDTO(true);
  }

  private void depositToAccount(
      final LiquidateAccountCommand liquidationCommand,
      final AccountDTO toInternalAccount,
      final AccountDTO syncedAccountDTO)
      throws AccountBusinessException {
    if (toInternalAccount != null) {
      LOG.info("Deposit dividends to internal account {} ", toInternalAccount);
      final var internalAccountId = toInternalAccount.id();
      final var totalAmount = liquidationCommand.currentBalance();
      final var transferDate = liquidationCommand.liquidatedDate();

      final AccountPK accountPK = new AccountPK(toInternalAccount.userId(), internalAccountId);
      final AccountMovementMetadata metadata = AccountMovementMetadata.createEmpty();
      metadata.putInvestmentIncomeAccount(syncedAccountDTO.toDomain());
      movementApplicationService.addDividendsMovement(
          accountPK, transferDate, totalAmount, null, null, metadata);
    }
  }
}

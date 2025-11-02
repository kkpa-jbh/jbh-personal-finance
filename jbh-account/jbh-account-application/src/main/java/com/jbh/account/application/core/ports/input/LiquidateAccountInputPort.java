package com.jbh.account.application.core.ports.input;

import com.jbh.account.application.acid.UnitOfWork;
import com.jbh.account.application.core.dto.AccountDTO;
import com.jbh.account.application.core.dto.LiquidationResultDTO;
import com.jbh.account.application.core.exceptions.BusinessApplicationExceptionType;
import com.jbh.account.application.core.mappers.MovementMapper;
import com.jbh.account.application.core.services.account.AccountService;
import com.jbh.account.application.core.services.monthlybalance.MonthlyBalanceService;
import com.jbh.account.application.core.services.movements.AccountMovementApplicationService;
import com.jbh.account.application.core.services.movements.AccountMovementService;
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
  private final AccountMovementService accountMovementService;
  private final UnitOfWork unitOfWork;
  private final MonthlyBalanceService monthlyBalanceService;
  private final AccountMovementApplicationService movementApplicationService;

  public LiquidateAccountInputPort(
      final AccountService accountService,
      final AccountMovementService accountMovementService,
      final MonthlyBalanceService monthlyBalanceService,
      final AccountMovementApplicationService movementApplicationService,
      final UnitOfWork unitOfWork) {
    this.unitOfWork = unitOfWork;
    this.accountMovementService = accountMovementService;
    this.accountService = accountService;
    this.monthlyBalanceService = monthlyBalanceService;
    this.movementApplicationService = movementApplicationService;
  }

  @Override
  public LiquidationResultDTO liquidateAccount(
      final UUID userId,
      final AccountId accountId,
      final LiquidateAccountCommand liquidationCommand)
      throws AccountBusinessException {

    final var movementDTO = MovementMapper.fromCommand(accountId, liquidationCommand);
    AccountDTO toInternalAccount = null;
    if (liquidationCommand.toInternalAccount().isPresent()) {
      final var internalAccountId = liquidationCommand.toInternalAccount().get().accountId();
      toInternalAccount = accountService.findAccountOrThrow(internalAccountId);
      movementDTO.metadata().putTargetInternalAccount(toInternalAccount.toDomain());
    }

    final AccountDTO syncedAccountDTO =
        accountService.syncByMovement(new AccountPK(userId, accountId), movementDTO, false);

    if (!syncedAccountDTO.isFullyWithdrawn()) {
      throw new AccountBusinessException(
          BusinessApplicationExceptionType.INVALID_LIQUIDATION_AMOUNT);
    }

    final var accountName = syncedAccountDTO.name();
    LOG.info("Liquidating account {} ", accountName);

    unitOfWork.execute(
        () -> {
          accountMovementService.save(movementDTO);
          accountService.save(syncedAccountDTO);
          LOG.info("Movement and Account {} persisted successfully", accountName);
        });

    LOG.info("Syncing Monthly Balance for liquidated account {}", accountName);
    monthlyBalanceService.syncForNewMovement(movementDTO);

    depositToAccount(liquidationCommand, toInternalAccount, syncedAccountDTO);

    return new LiquidationResultDTO(true);
  }

  private void depositToAccount(
      final LiquidateAccountCommand liquidationCommand,
      final AccountDTO toInternalAccount,
      final AccountDTO syncedAccountDTO)
      throws AccountBusinessException {
    if (toInternalAccount != null) {
      final var internalAccountId = toInternalAccount.id();
      final var totalAmount = liquidationCommand.totalAmount();
      final var transferDate = liquidationCommand.transferDate();

      final AccountPK accountPK = new AccountPK(toInternalAccount.userId(), internalAccountId);
      final AccountMovementMetadata metadata = AccountMovementMetadata.createEmpty();
      metadata.putInvestmentIncomeAccount(syncedAccountDTO.toDomain());
      movementApplicationService.addDividendsMovement(
          accountPK, transferDate, totalAmount, null, null, metadata);
    }
  }
}

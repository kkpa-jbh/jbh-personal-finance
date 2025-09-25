package com.jbh.account.application.core.ports.input;

import static com.jbh.account.application.core.mappers.MonthlyBalanceMapper.toDTO;

import com.jbh.account.application.common.logging.LoggerFactory;
import com.jbh.account.application.common.logging.LoggingContext;
import com.jbh.account.application.core.dto.AddBasicMovementDTO;
import com.jbh.account.application.core.mappers.MovementMapper;
import com.jbh.account.application.core.services.MonthlyBalanceAsyncTask;
import com.jbh.account.application.core.services.movements.AccountMovementService;
import com.jbh.account.application.core.usecases.AddMovementUseCase;
import com.jbh.account.application.core.vo.commands.AddMovementCommand;
import com.jbh.account.domain.entity.AccountMonthlyBalanceDomain;
import com.jbh.account.domain.vo.AccountId;
import com.jbh.account.domain.vo.AccountPK;
import java.util.UUID;
import org.slf4j.Logger;

public class AddMovementInputPort implements AddMovementUseCase {

  private static final Logger LOG = LoggerFactory.getLogger(AddMovementInputPort.class);
  private final AccountMovementService accountMovementService;
  private final MonthlyBalanceAsyncTask monthlyBalanceSyncerService;

  public AddMovementInputPort(
      final AccountMovementService accountMovementService,
      final MonthlyBalanceAsyncTask monthlyBalanceSyncerService) {
    this.accountMovementService = accountMovementService;
    this.monthlyBalanceSyncerService = monthlyBalanceSyncerService;
  }

  @Override
  public AddBasicMovementDTO addMovement(
      final UUID userId, final AccountId accountId, final AddMovementCommand movementCommand) {
    return LoggingContext.builder()
        .accountId(accountId.value())
        .userId(userId)
        .module("account-application")
        .execute(
            () -> {

              // Sync account balance and persist movement
              final AddBasicMovementDTO addBasicMovementDTO =
                  accountMovementService.addMovement(
                      new AccountPK(userId, accountId), movementCommand);

              // Sync monthly balance asynchronously
              // FIXME: Check if can be done using DTO
              final AccountMonthlyBalanceDomain accountMonthlyBalance =
                  monthlyBalanceSyncerService.syncForNewMovement(
                      MovementMapper.toDomain(addBasicMovementDTO.movement()));

              LOG.info(
                  "Movement addition completed successfully for account: {}", accountId.value());

              return new AddBasicMovementDTO(
                  addBasicMovementDTO.account(),
                  toDTO(accountMonthlyBalance),
                  addBasicMovementDTO.movement());
            });
  }
}

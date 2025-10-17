package com.jbh.account.application.core.ports.input;

import com.jbh.account.application.common.logging.LoggerFactory;
import com.jbh.account.application.core.dto.AddBasicMovementDTO;
import com.jbh.account.application.core.services.movements.AccountMovementService;
import com.jbh.account.application.core.usecases.AddMovementUseCase;
import com.jbh.account.application.core.vo.commands.AddMovementCommand;
import com.jbh.account.domain.exceptions.AccountBusinessException;
import com.jbh.account.domain.vo.AccountId;
import com.jbh.account.domain.vo.AccountPK;
import java.util.UUID;
import org.slf4j.Logger;

public class AddMovementInputPort implements AddMovementUseCase {

  private static final Logger LOG = LoggerFactory.getLogger(AddMovementInputPort.class);
  private final AccountMovementService accountMovementService;

  public AddMovementInputPort(final AccountMovementService accountMovementService) {
    this.accountMovementService = accountMovementService;
  }

  @Override
  public AddBasicMovementDTO addMovement(
      final UUID userId, final AccountId accountId, final AddMovementCommand movementCommand)
      throws AccountBusinessException {
    // Sync account balance and persist movement
    final AddBasicMovementDTO addBasicMovementDTO;
    addBasicMovementDTO =
        accountMovementService.addMovementProcessingBalances(
            new AccountPK(userId, accountId), movementCommand);

    LOG.info("Movement addition completed successfully for account: {}", accountId.value());

    return addBasicMovementDTO;
  }
}

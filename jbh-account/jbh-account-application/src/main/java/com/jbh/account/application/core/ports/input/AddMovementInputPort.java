package com.jbh.account.application.core.ports.input;

import com.jbh.account.application.core.dto.AddBasicMovementDTO;
import com.jbh.account.application.core.services.movements.AccountMovementApplicationService;
import com.jbh.account.application.core.usecases.AddMovementUseCase;
import com.jbh.account.application.core.vo.commands.AddMovementCommand;
import com.jbh.account.domain.exceptions.AccountBusinessException;
import com.jbh.account.domain.vo.AccountId;
import com.jbh.account.domain.vo.AccountPK;
import java.util.UUID;

public class AddMovementInputPort implements AddMovementUseCase {

  // private static final Logger LOG = LoggerFactory.getLogger(AddMovementInputPort.class);
  private final AccountMovementApplicationService accountMovementService;

  public AddMovementInputPort(final AccountMovementApplicationService accountMovementService) {
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

    return addBasicMovementDTO;
  }
}

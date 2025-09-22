package com.jbh.account.application.core.usecases;

import com.jbh.account.application.core.dto.AddMultipleBasicMovementDTO;
import com.jbh.account.application.core.vo.commands.AddMovementUploadedFileCommand;
import com.jbh.account.domain.vo.AccountId;
import java.util.List;
import java.util.UUID;

public interface AddMovementsUploadedFileUseCase {

  AddMultipleBasicMovementDTO uploadMovementsFromFile(
      UUID userId, AccountId accountId, List<AddMovementUploadedFileCommand> allUploadedMovements);
}

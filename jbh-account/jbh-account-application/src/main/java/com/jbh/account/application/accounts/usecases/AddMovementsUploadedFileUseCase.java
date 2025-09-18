package com.jbh.account.application.accounts.usecases;

import com.jbh.account.application.accounts.dto.AddMultipleBasicMovementDTO;
import com.jbh.account.application.accounts.vo.commands.AddMovementUploadedFileCommand;
import com.jbh.account.domain.vo.AccountId;
import java.util.List;
import java.util.UUID;

public interface AddMovementsUploadedFileUseCase {

  AddMultipleBasicMovementDTO uploadMovementsFromFile(
      UUID userId, AccountId accountId, List<AddMovementUploadedFileCommand> allUploadedMovements);
}

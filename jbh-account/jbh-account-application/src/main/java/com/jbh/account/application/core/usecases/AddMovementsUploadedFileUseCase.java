package com.jbh.account.application.core.usecases;

import com.jbh.account.application.core.dto.AddMultipleBasicMovementDTO;
import com.jbh.account.application.core.vo.commands.AddMovementUploadedFileCommand;
import com.jbh.account.domain.vo.ProductId;
import com.jbh.commons.exception.BusinessException;
import java.util.List;
import java.util.UUID;

public interface AddMovementsUploadedFileUseCase {

  AddMultipleBasicMovementDTO uploadMovementsFromFile(
      UUID userId, ProductId accountId, List<AddMovementUploadedFileCommand> allUploadedMovements)
      throws BusinessException;
}

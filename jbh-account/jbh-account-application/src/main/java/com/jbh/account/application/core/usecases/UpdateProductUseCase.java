package com.jbh.account.application.core.usecases;

import com.jbh.account.application.core.vo.commands.UpdateMetadataProductCommand;
import com.jbh.account.domain.exceptions.AccountBusinessException;
import com.jbh.account.domain.vo.AccountPK;

public interface UpdateProductUseCase {

  void replaceMetadata(AccountPK accountPK, UpdateMetadataProductCommand command)
      throws AccountBusinessException;
}

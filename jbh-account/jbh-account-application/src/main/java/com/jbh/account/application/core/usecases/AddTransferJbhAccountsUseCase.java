package com.jbh.account.application.core.usecases;

import com.jbh.account.application.core.vo.commands.AddTransferCommand;
import com.jbh.account.domain.vo.ProductPK;
import com.jbh.commons.exception.BusinessException;

/**
 * Use case to add a transfer between existing accounts into the system. If the transfer is to an
 * unknown account, that action should be done via Add Movement Use Case.
 */
public interface AddTransferJbhAccountsUseCase {

  void addTransfer(ProductPK fromAccount, AddTransferCommand transferCommand)
      throws BusinessException;
}

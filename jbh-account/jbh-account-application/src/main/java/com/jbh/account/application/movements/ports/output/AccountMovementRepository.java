package com.jbh.account.application.movements.ports.output;

import com.jbh.accounts_mgmt.movements.AccountMovementDomain;
import java.util.List;

public interface AccountMovementRepository {

  AccountMovementDomain save(AccountMovementDomain accountMovement);

  void save(List<AccountMovementDomain> newMovements);
}

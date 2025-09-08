package com.jbh.account.application.movements.ports.output;

import com.jbh.account.domain.movements.AccountMovementDomain;
import java.util.List;

public interface AccountMovementRepository {

  AccountMovementDomain save(AccountMovementDomain accountMovement);

  void save(List<AccountMovementDomain> newMovements);
}

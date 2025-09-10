package com.jbh.account.application.movements.ports.output;

import com.jbh.account.domain.entity.AccountMovementDomain;
import java.util.List;

public interface AccountMovementRepository {

  AccountMovementDomain save(AccountMovementDomain accountMovement);

  void save(List<AccountMovementDomain> newMovements);
}

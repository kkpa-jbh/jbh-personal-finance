package com.jbh.account.application.movements.ports.output;

import com.jbh.accounts_mgmt.movements.AccountMovementDomain;

public interface AccountMovementRepository {

  AccountMovementDomain save(AccountMovementDomain accountMovement);
}

package com.jbh.account_app.movements.ports.output;

import com.jbh.accounts_mgmt.movements.AccountMovement;

public interface AccountMovementRepository {

  AccountMovement save(AccountMovement accountMovement);
}

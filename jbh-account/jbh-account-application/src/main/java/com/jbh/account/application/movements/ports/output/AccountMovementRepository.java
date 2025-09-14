package com.jbh.account.application.movements.ports.output;

import com.jbh.account.domain.vo.AccountMovementDTO;
import java.util.List;

public interface AccountMovementRepository {

  void save(AccountMovementDTO accountMovement);

  void save(List<AccountMovementDTO> newMovements);
}

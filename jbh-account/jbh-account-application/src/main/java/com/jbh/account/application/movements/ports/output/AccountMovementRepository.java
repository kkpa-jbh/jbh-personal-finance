package com.jbh.account.application.movements.ports.output;

import com.jbh.account.application.core.dto.MovementDTO;
import java.util.List;

public interface AccountMovementRepository {

  void save(MovementDTO accountMovement);

  void save(List<MovementDTO> newMovements);
}

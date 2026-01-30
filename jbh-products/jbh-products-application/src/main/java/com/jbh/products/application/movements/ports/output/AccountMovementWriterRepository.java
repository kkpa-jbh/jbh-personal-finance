package com.jbh.products.application.movements.ports.output;

import com.jbh.products.application.core.dto.MovementDTO;
import java.util.List;

public interface AccountMovementWriterRepository {

  void save(MovementDTO accountMovement);

  void save(List<MovementDTO> newMovements);
}
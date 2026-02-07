package com.jbh.products.application.feature.movement.ports.output;

import com.jbh.products.application.feature.movement.dto.MovementDTO;
import java.util.List;

public interface AccountMovementWriterRepository {

  void save(MovementDTO accountMovement);

  void save(List<MovementDTO> newMovements);
}
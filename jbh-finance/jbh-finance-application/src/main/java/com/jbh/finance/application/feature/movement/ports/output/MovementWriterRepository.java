package com.jbh.finance.application.feature.movement.ports.output;

import com.jbh.finance.application.feature.movement.dto.MovementDTO;
import java.util.List;

import java.util.UUID;

public interface MovementWriterRepository {

  void save(MovementDTO accountMovement);

  void save(List<MovementDTO> newMovements);

  void delete(UUID movementId);
}

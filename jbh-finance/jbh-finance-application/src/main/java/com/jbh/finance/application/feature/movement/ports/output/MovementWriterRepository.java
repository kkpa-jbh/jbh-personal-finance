package com.jbh.finance.application.feature.movement.ports.output;

import com.jbh.finance.application.feature.movement.dto.MovementDTO;
import java.util.List;

public interface MovementWriterRepository {

  void save(MovementDTO accountMovement);

  void save(List<MovementDTO> newMovements);
}

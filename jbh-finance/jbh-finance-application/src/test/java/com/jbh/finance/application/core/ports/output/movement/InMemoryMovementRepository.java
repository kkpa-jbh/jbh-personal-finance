package com.jbh.finance.application.core.ports.output.movement;

import com.jbh.finance.application.feature.movement.dto.MovementDTO;
import com.jbh.finance.application.feature.movement.ports.output.MovementWriterRepository;
import java.util.List;

public class InMemoryMovementRepository implements MovementWriterRepository {

  private final InMemoryMovementQueryRepository queryRepo;

  public InMemoryMovementRepository(final InMemoryMovementQueryRepository queryRepo) {
    this.queryRepo = queryRepo;
  }

  @Override
  public void save(final MovementDTO accountMovement) {
    queryRepo.save(accountMovement);
  }

  @Override
  public void save(final List<MovementDTO> newMovements) {
    queryRepo.saveAll(newMovements);
  }
}

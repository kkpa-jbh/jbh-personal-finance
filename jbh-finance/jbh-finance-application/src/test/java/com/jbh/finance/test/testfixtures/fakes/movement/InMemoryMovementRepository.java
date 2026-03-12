package com.jbh.finance.test.testfixtures.fakes.movement;

import com.jbh.finance.application.feature.movement.dto.MovementDTO;
import com.jbh.finance.application.feature.movement.ports.output.MovementWriterRepository;
import java.util.List;
import java.util.UUID;

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

  @Override
  public void delete(final UUID movementId) {
    queryRepo.deleteById(movementId);
  }
}

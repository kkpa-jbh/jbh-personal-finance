package com.jbh.products.application.core.ports.output.movement;

import com.jbh.products.application.core.dto.MovementDTO;
import com.jbh.products.application.movements.ports.output.AccountMovementWriterRepository;
import java.util.List;

public class InMemoryAccountMovementRepository implements AccountMovementWriterRepository {

  private final InMemoryAccountMovementQueryRepository queryRepo;

  public InMemoryAccountMovementRepository(
      final InMemoryAccountMovementQueryRepository queryRepo) {
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
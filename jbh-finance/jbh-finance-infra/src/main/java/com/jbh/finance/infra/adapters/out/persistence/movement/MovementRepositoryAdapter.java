package com.jbh.finance.infra.adapters.out.persistence.movement;

import com.jbh.finance.application.feature.movement.dto.MovementDTO;
import com.jbh.finance.application.feature.movement.ports.output.MovementWriterRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import java.util.List;

@ApplicationScoped
public class MovementRepositoryAdapter implements MovementWriterRepository {

  @Inject MovementJPARepository jpaRepo;

  @Override
  public void save(final List<MovementDTO> newMovements) {
    newMovements.forEach(this::save);
  }

  @Override
  @Transactional
  public void save(final MovementDTO accountMovement) {
    final MovementJPAEntity entity = MovementJPAEntity.toEntity(accountMovement);
    if (entity.getId() == null) {
      throw new IllegalStateException(" movement ID cannot be null");
    }

    jpaRepo.getEntityManager().persist(entity);
  }
}

package com.jbh.finance.infra.adapters.out.persistence.movement;

import com.jbh.finance.application.feature.movement.dto.MovementDTO;
import com.jbh.finance.application.feature.movement.ports.output.MovementWriterRepository;
import com.jbh.finance.infra.adapters.out.persistence.category.CategoryJPAEntity;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
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

    final EntityManager em = jpaRepo.getEntityManager();
    if (accountMovement.category() != null && accountMovement.category().getCategoryId() != null) {
      entity.setCategory(em.getReference(CategoryJPAEntity.class, accountMovement.category().getCategoryId()));
    }

    em.persist(entity);
  }
}

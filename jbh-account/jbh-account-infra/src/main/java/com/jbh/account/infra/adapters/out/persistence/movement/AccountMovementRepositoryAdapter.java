package com.jbh.account.infra.adapters.out.persistence.movement;

import com.jbh.account.application.core.dto.MovementDTO;
import com.jbh.account.application.movements.ports.output.AccountMovementRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import java.util.List;

@ApplicationScoped
public class AccountMovementRepositoryAdapter implements AccountMovementRepository {

  @Inject AccountMovementJPARepository jpaRepo;

  @Override
  public void save(final List<MovementDTO> newMovements) {
    newMovements.forEach(this::save);
  }

  @Override
  @Transactional
  public void save(final MovementDTO accountMovement) {
    final AccountMovementJPAEntity entity = AccountMovementJPAEntity.toEntity(accountMovement);
    if (entity.getId() == null) {
      throw new IllegalStateException("Account movement ID cannot be null");
    }

    jpaRepo.getEntityManager().persist(entity);
  }
}

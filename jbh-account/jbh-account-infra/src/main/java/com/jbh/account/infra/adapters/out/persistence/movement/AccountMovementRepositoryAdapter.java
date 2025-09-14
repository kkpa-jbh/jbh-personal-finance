package com.jbh.account.infra.adapters.out.persistence.movement;

import com.jbh.account.application.movements.ports.output.AccountMovementRepository;
import com.jbh.account.domain.vo.AccountMovementDTO;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import java.util.List;

@ApplicationScoped
public class AccountMovementRepositoryAdapter implements AccountMovementRepository {

  @Inject AccountMovementJPARepository jpaRepo;

  @Override
  @Transactional
  public void save(final AccountMovementDTO accountMovement) {
    final AccountMovementJPAEntity entity = AccountMovementJPAEntity.of(accountMovement);

    // If ID is null, it's a new entity - use persist
    // If ID is set, it's an existing entity - use merge
    if (entity.getId() == null) {
      jpaRepo.persist(entity);
    } else {
      jpaRepo.getEntityManager().merge(entity);
    }
  }

  @Override
  public void save(final List<AccountMovementDTO> newMovements) {
    newMovements.forEach(this::save);
  }
}

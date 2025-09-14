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
    if (entity.getId() == null) {
      throw new IllegalStateException("Account movement ID cannot be null");
    }

    jpaRepo.getEntityManager().persist(entity);
  }

  @Override
  public void save(final List<AccountMovementDTO> newMovements) {
    newMovements.forEach(this::save);
  }
}

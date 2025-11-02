package com.jbh.account.infra.adapters.out.persistence.movement;

import com.jbh.account.application.core.dto.MovementDTO;
import com.jbh.account.application.movements.ports.output.AccountMovementQueryRepository;
import com.jbh.account.domain.vo.AccountId;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.util.List;

@ApplicationScoped
public class AccountMovementRepositoryQueryAdapter implements AccountMovementQueryRepository {

  @Inject AccountMovementJPARepository jpaRepo;

  @Override
  public List<MovementDTO> findByAccountId(final AccountId accountId) {
    return jpaRepo.findByAccountId(accountId.value());
  }
}

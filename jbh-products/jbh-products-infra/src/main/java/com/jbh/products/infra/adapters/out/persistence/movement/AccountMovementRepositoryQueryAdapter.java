package com.jbh.products.infra.adapters.out.persistence.movement;

import com.jbh.products.application.feature.movement.dto.MovementDTO;
import com.jbh.products.application.feature.movement.ports.output.AccountMovementQueryRepository;
import com.jbh.products.domain.product.vo.ProductId;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.util.List;

@ApplicationScoped
public class AccountMovementRepositoryQueryAdapter implements AccountMovementQueryRepository {

  @Inject AccountMovementJPARepository jpaRepo;

  @Override
  public List<MovementDTO> findByAccountId(final ProductId accountId) {
    return jpaRepo.findByAccountId(accountId.value());
  }
}

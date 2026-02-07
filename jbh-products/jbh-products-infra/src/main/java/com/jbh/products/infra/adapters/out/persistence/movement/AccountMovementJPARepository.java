package com.jbh.products.infra.adapters.out.persistence.movement;

import com.jbh.products.application.feature.movement.dto.MovementDTO;
import io.quarkus.hibernate.orm.panache.PanacheRepository;
import io.quarkus.panache.common.Parameters;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.persistence.PersistenceUnit;
import java.util.List;
import java.util.UUID;

@ApplicationScoped
@PersistenceUnit(name = "finance")
public class AccountMovementJPARepository implements PanacheRepository<MovementJPAEntity> {

  private static final String ACCOUNT_ID_PARAM = "accountId";

  public List<MovementDTO> findByAccountId(final UUID accountId) {
    return find("accountId = :accountId", Parameters.with(ACCOUNT_ID_PARAM, accountId))
        .list()
        .stream()
        .map(MovementJPAEntity::toDTO)
        .toList();
  }
}

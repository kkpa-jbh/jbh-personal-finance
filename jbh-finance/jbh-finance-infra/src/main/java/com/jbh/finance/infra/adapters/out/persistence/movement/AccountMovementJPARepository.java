package com.jbh.finance.infra.adapters.out.persistence.movement;

import com.jbh.finance.application.feature.movement.dto.MovementDTO;
import io.quarkus.hibernate.orm.panache.PanacheRepository;
import io.quarkus.panache.common.Parameters;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.persistence.PersistenceUnit;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@ApplicationScoped
@PersistenceUnit(name = "finance")
public class AccountMovementJPARepository implements PanacheRepository<MovementJPAEntity> {

  private static final String ACCOUNT_ID_PARAM = "accountId";
  private static final String START_DATE_PARAM = "startDate";
  private static final String END_DATE_PARAM = "endDate";

  public List<MovementDTO> findByAccountId(final UUID accountId) {
    return find("accountId = :accountId", Parameters.with(ACCOUNT_ID_PARAM, accountId))
        .list()
        .stream()
        .map(MovementJPAEntity::toDTO)
        .toList();
  }

  public List<MovementDTO> findByProductIdAndDateRange(
      final UUID productId, final LocalDate startDate, final LocalDate endDate) {
    return find(
            "accountId = :accountId AND movementDate >= :startDate AND movementDate <= :endDate",
            Parameters.with(ACCOUNT_ID_PARAM, productId)
                .and(START_DATE_PARAM, startDate)
                .and(END_DATE_PARAM, endDate))
        .list()
        .stream()
        .map(MovementJPAEntity::toDTO)
        .toList();
  }
}

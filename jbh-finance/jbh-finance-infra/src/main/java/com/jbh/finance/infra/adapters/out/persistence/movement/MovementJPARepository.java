package com.jbh.finance.infra.adapters.out.persistence.movement;

import com.jbh.finance.application.feature.movement.dto.MovementDTO;
import io.quarkus.hibernate.orm.panache.PanacheRepository;
import io.quarkus.panache.common.Parameters;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.persistence.PersistenceUnit;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@ApplicationScoped
@PersistenceUnit(name = "finance")
public class MovementJPARepository implements PanacheRepository<MovementJPAEntity> {

  private static final String PRODUCT_ID_PARAM = "productId";
  private static final String START_DATE_PARAM = "startDate";
  private static final String END_DATE_PARAM = "endDate";
  private static final String MOVEMENT_ID = "id";

  public List<MovementDTO> findByProductId(final UUID accountId) {
    return find("productId = :productId", Parameters.with(PRODUCT_ID_PARAM, accountId))
        .list()
        .stream()
        .map(MovementJPAEntity::toDTO)
        .toList();
  }

  public List<MovementDTO> findByProductIdAndDateRange(
      final UUID productId, final LocalDate startDate, final LocalDate endDate) {
    return find(
            "productId = :productId AND movementDate >= :startDate AND movementDate <= :endDate",
            Parameters.with(PRODUCT_ID_PARAM, productId)
                .and(START_DATE_PARAM, startDate)
                .and(END_DATE_PARAM, endDate))
        .list()
        .stream()
        .map(MovementJPAEntity::toDTO)
        .toList();
  }

  public Optional<MovementJPAEntity> findById(final UUID movementId) {
    return find("id = :movementId", Parameters.with(MOVEMENT_ID, movementId))
        .singleResultOptional();
  }

  public boolean deleteById(final UUID uuid) {
    return delete("id = :movementId", Parameters.with("movementId", uuid)) != 0;
  }
}

package com.jbh.finance.test.testfixtures.fakes.movement;

import com.jbh.finance.application.feature.movement.dto.MovementDTO;
import com.jbh.finance.application.feature.movement.ports.output.MovementQueryRepository;
import com.jbh.finance.domain.product.vo.ProductId;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class InMemoryMovementQueryRepository implements MovementQueryRepository {

  private static final Logger log = LoggerFactory.getLogger(InMemoryMovementQueryRepository.class);
  private final Map<UUID, MovementDTO> storage = new HashMap<>();

  public void saveAll(final List<MovementDTO> newMovements) {
    newMovements.forEach(this::save);
    log.warn("Saved {} movements", newMovements.size());
  }

  public void save(final MovementDTO accountMovement) {
    storage.put(accountMovement.id().value(), accountMovement);
    log.warn("Saved Movement " + accountMovement);
  }

  @Override
  public List<MovementDTO> getByProductId(final ProductId accountId) {
    return storage.values().stream()
        .filter(movement -> movement.productId().equals(accountId))
        .sorted((m1, m2) -> m1.movementDate().compareTo(m2.movementDate()))
        .toList();
  }

  @Override
  public List<MovementDTO> getByUserAndProductIdWithinPeriod(
      final UUID userId,
      final ProductId productId,
      final LocalDate startDate,
      final LocalDate endDate) {
    return storage.values().stream()
        .filter(movement -> movement.productId().equals(productId))
        .filter(
            movement ->
                !movement.movementDate().isBefore(startDate)
                    && !movement.movementDate().isAfter(endDate))
        .sorted((m1, m2) -> m2.movementDate().compareTo(m1.movementDate()))
        .toList();
  }

  @Override
  public Optional<MovementDTO> findById(final UUID movementId) {
    return Optional.ofNullable(storage.get(movementId));
  }

  public void clearStorage() {
    storage.clear();
  }

  public int size() {
    return storage.size();
  }

  public List<MovementDTO> findAll() {
    return new ArrayList<>(storage.values());
  }

  public void deleteById(final UUID movementId) {
    storage.remove(movementId);
  }
}

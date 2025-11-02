package com.jbh.account.application.core.ports.output.movement;

import com.jbh.account.application.core.dto.MovementDTO;
import com.jbh.account.application.movements.ports.output.AccountMovementQueryRepository;
import com.jbh.account.domain.vo.AccountId;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class InMemoryAccountMovementQueryRepository implements AccountMovementQueryRepository {

  private static final Logger log =
      LoggerFactory.getLogger(InMemoryAccountMovementQueryRepository.class);
  private final Map<UUID, MovementDTO> storage = new HashMap<>();

  public void save(final MovementDTO accountMovement) {
    storage.put(accountMovement.id().value(), accountMovement);
    log.warn("Saved Movement " + accountMovement);
  }

  public void saveAll(final List<MovementDTO> newMovements) {
    newMovements.forEach(this::save);
    log.warn("Saved {} movements", newMovements.size());
  }

  @Override
  public List<MovementDTO> findByAccountId(final AccountId accountId) {
    return storage.values().stream()
        .filter(movement -> movement.accountId().equals(accountId))
        .sorted((m1, m2) -> m1.movementDate().compareTo(m2.movementDate()))
        .toList();
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
}
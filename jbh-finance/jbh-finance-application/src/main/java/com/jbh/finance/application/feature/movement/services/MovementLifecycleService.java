package com.jbh.finance.application.feature.movement.services;

import com.jbh.finance.application.feature.movement.dto.MovementDTO;
import com.jbh.finance.domain.product.vo.ProductId;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface MovementLifecycleService {

  void save(MovementDTO movementDTO);

  List<MovementDTO> findByAccountId(ProductId id);

  List<MovementDTO> getByUserAndProductIdWithinPeriod(
      UUID userId, ProductId productId, LocalDate startDate, LocalDate endDate);

  Optional<MovementDTO> findById(UUID movementId);

  void delete(UUID movementId);
}

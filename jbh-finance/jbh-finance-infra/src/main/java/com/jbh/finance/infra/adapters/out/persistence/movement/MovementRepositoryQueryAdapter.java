package com.jbh.finance.infra.adapters.out.persistence.movement;

import com.jbh.finance.application.feature.movement.dto.MovementDTO;
import com.jbh.finance.application.feature.movement.ports.output.MovementQueryRepository;
import com.jbh.finance.domain.product.vo.ProductId;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@ApplicationScoped
public class MovementRepositoryQueryAdapter implements MovementQueryRepository {

  @Inject MovementJPARepository jpaRepo;

  @Override
  public List<MovementDTO> getByProductId(final ProductId accountId) {
    return jpaRepo.findByProductId(accountId.value());
  }

  @Override
  public List<MovementDTO> getByUserAndProductIdWithinPeriod(
      final UUID userId,
      final ProductId productId,
      final LocalDate startDate,
      final LocalDate endDate) {
    return jpaRepo.findByProductIdAndDateRange(productId.value(), startDate, endDate);
  }
}

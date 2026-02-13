package com.jbh.finance.application.feature.movement.services;

import com.jbh.finance.application.feature.movement.dto.MovementDTO;
import com.jbh.finance.application.feature.movement.ports.output.MovementQueryRepository;
import com.jbh.finance.application.feature.movement.ports.output.MovementWriterRepository;
import com.jbh.finance.domain.product.vo.ProductId;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

public class MovementLifecycleServiceImpl implements MovementLifecycleService {
  private final MovementWriterRepository movementWriterRepo;
  private final MovementQueryRepository movementQueryRepo;

  public MovementLifecycleServiceImpl(
      final MovementWriterRepository movementWriterRepo,
      final MovementQueryRepository movementQueryRepo) {
    this.movementWriterRepo = movementWriterRepo;
    this.movementQueryRepo = movementQueryRepo;
  }

  @Override
  public void save(final MovementDTO movementDTO) {
    movementWriterRepo.save(movementDTO);
  }

  @Override
  public List<MovementDTO> findByAccountId(final ProductId id) {
    return movementQueryRepo.getByProductId(id);
  }

  @Override
  public List<MovementDTO> getByUserAndProductIdWithinPeriod(
      final UUID userId,
      final ProductId productId,
      final LocalDate startDate,
      final LocalDate endDate) {

    final YearMonth currentMonth = YearMonth.now();

    return movementQueryRepo
        .getByUserAndProductIdWithinPeriod(userId, productId, startDate, endDate)
        .stream()
        .map(
            movementDTO -> {
              if (movementDTO.createdAt() != null) {
                final YearMonth createdMonth = YearMonth.from(movementDTO.createdAt());
                if (currentMonth.equals(createdMonth)) {
                  movementDTO.setToRemoval(true);
                }
              }
              return movementDTO;
            })
        .collect(Collectors.toUnmodifiableList());
  }
}

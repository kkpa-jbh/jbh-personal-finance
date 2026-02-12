package com.jbh.finance.application.feature.movement.services;

import com.jbh.finance.application.feature.movement.dto.MovementDTO;
import com.jbh.finance.application.feature.movement.ports.output.AccountMovementQueryRepository;
import com.jbh.finance.application.feature.movement.ports.output.AccountMovementWriterRepository;
import com.jbh.finance.domain.product.vo.ProductId;
import java.util.List;

public class AccountMovementServiceImpl implements MovementService {
  private final AccountMovementWriterRepository movementWriterRepo;
  private final AccountMovementQueryRepository movementQueryRepo;

  public AccountMovementServiceImpl(
      final AccountMovementWriterRepository movementWriterRepo,
      final AccountMovementQueryRepository movementQueryRepo) {
    this.movementWriterRepo = movementWriterRepo;
    this.movementQueryRepo = movementQueryRepo;
  }

  @Override
  public void save(final MovementDTO movementDTO) {
    movementWriterRepo.save(movementDTO);
  }

  @Override
  public List<MovementDTO> findByAccountId(final ProductId id) {
    return movementQueryRepo.findByAccountId(id);
  }
}

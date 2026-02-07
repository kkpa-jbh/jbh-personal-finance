package com.jbh.products.application.feature.movement.services;

import com.jbh.products.application.feature.movement.dto.MovementDTO;
import com.jbh.products.application.feature.movement.ports.output.AccountMovementQueryRepository;
import com.jbh.products.application.feature.movement.ports.output.AccountMovementWriterRepository;
import com.jbh.products.domain.product.vo.ProductId;
import java.util.List;

public class AccountMovementServiceImpl implements AccountMovementService {
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

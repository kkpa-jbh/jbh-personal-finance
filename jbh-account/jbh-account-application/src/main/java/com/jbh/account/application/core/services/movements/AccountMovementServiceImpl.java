package com.jbh.account.application.core.services.movements;

import com.jbh.account.application.core.dto.MovementDTO;
import com.jbh.account.application.movements.ports.output.AccountMovementQueryRepository;
import com.jbh.account.application.movements.ports.output.AccountMovementWriterRepository;
import com.jbh.account.domain.vo.ProductId;
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

package com.jbh.finance.application.feature.movement.services;

import com.jbh.finance.application.feature.movement.dto.MovementDTO;
import com.jbh.finance.domain.product.vo.ProductId;
import java.util.List;

public interface AccountMovementService {

  void save(MovementDTO movementDTO);

  List<MovementDTO> findByAccountId(ProductId id);
}

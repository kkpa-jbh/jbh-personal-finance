package com.jbh.products.application.feature.movement.services;

import com.jbh.products.application.feature.movement.dto.MovementDTO;
import com.jbh.products.domain.product.vo.ProductId;
import java.util.List;

public interface AccountMovementService {

  void save(MovementDTO movementDTO);

  List<MovementDTO> findByAccountId(ProductId id);
}

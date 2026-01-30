package com.jbh.products.application.core.services.movements;

import com.jbh.products.application.core.dto.MovementDTO;
import com.jbh.products.domain.vo.ProductId;
import java.util.List;

public interface AccountMovementService {

  void save(MovementDTO movementDTO);

  List<MovementDTO> findByAccountId(ProductId id);
}

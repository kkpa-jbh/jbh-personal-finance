package com.jbh.account.application.core.services.movements;

import com.jbh.account.application.core.dto.MovementDTO;
import com.jbh.account.domain.vo.ProductId;
import java.util.List;

public interface AccountMovementService {

  void save(MovementDTO movementDTO);

  List<MovementDTO> findByAccountId(ProductId id);
}

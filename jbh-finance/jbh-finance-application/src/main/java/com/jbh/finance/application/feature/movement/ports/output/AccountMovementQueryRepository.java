package com.jbh.finance.application.feature.movement.ports.output;

import com.jbh.finance.application.feature.movement.dto.MovementDTO;
import com.jbh.finance.domain.product.vo.ProductId;
import java.util.List;

public interface AccountMovementQueryRepository {

  List<MovementDTO> findByAccountId(ProductId accountId);
}
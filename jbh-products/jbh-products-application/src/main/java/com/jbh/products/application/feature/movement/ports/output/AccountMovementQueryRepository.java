package com.jbh.products.application.feature.movement.ports.output;

import com.jbh.products.application.feature.movement.dto.MovementDTO;
import com.jbh.products.domain.product.vo.ProductId;
import java.util.List;

public interface AccountMovementQueryRepository {

  List<MovementDTO> findByAccountId(ProductId accountId);
}
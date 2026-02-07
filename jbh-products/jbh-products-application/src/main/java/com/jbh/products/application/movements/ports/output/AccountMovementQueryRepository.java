package com.jbh.products.application.movements.ports.output;

import com.jbh.products.application.core.dto.MovementDTO;
import com.jbh.products.domain.product.vo.ProductId;
import java.util.List;

public interface AccountMovementQueryRepository {

  List<MovementDTO> findByAccountId(ProductId accountId);
}
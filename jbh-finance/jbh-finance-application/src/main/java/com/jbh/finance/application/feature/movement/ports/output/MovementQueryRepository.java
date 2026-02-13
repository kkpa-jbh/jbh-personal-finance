package com.jbh.finance.application.feature.movement.ports.output;

import com.jbh.finance.application.feature.movement.dto.MovementDTO;
import com.jbh.finance.domain.product.vo.ProductId;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public interface MovementQueryRepository {

  List<MovementDTO> getByProductId(ProductId accountId);

  List<MovementDTO> getByUserAndProductIdWithinPeriod(
      UUID userId, ProductId productId, LocalDate startDate, LocalDate endDate);
}

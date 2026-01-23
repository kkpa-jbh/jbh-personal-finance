package com.jbh.account.application.movements.ports.output;

import com.jbh.account.application.core.dto.MovementDTO;
import com.jbh.account.domain.vo.ProductId;
import java.util.List;

public interface AccountMovementQueryRepository {

  List<MovementDTO> findByAccountId(ProductId accountId);
}
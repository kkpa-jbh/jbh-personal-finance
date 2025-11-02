package com.jbh.account.application.movements.ports.output;

import com.jbh.account.application.core.dto.MovementDTO;
import com.jbh.account.domain.vo.AccountId;
import java.util.List;

public interface AccountMovementQueryRepository {

  List<MovementDTO> findByAccountId(AccountId accountId);
}
package com.jbh.finance.infra.adapters.in.rest.movement.request;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public record LiquidateAccountRequest(
    UUID toInternalAccountId,
    String toExternalAccountOwner,
    BigDecimal currentBalance,
    LocalDate liquidatedDate) {}

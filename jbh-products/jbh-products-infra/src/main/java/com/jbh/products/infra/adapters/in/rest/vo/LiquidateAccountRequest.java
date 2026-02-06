package com.jbh.products.infra.adapters.in.rest.vo;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public record LiquidateAccountRequest(
    UUID toInternalAccountId,
    String toExternalAccountOwner,
    BigDecimal currentBalance,
    LocalDate liquidatedDate) {}

package com.jbh.products.infra.adapters.in.rest.vo;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public record AddTransferRequest(
    UUID toAccountId, BigDecimal totalAmount, LocalDate transferDate) {}

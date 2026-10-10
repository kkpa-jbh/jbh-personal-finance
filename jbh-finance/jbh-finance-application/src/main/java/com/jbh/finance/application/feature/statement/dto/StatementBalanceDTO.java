package com.jbh.finance.application.feature.statement.dto;

import java.math.BigDecimal;

/**
 * The balances a statement prints for one currency. A statement can have one section per
 * currency, each with its own balances.
 *
 * <p>On a credit card statement the balances are the debt: opening is the previous balance, closing
 * is the total balance at the cut-off date.
 *
 * @param currency ISO 4217 code, for example {@code COP}
 * @param openingBalance balance at the start of the period, or {@code null} when not shown
 * @param closingBalance balance at the end of the period, or {@code null} when not shown
 */
public record StatementBalanceDTO(
    String currency, BigDecimal openingBalance, BigDecimal closingBalance) {}

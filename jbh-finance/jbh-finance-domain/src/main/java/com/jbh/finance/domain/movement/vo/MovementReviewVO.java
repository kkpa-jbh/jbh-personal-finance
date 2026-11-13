package com.jbh.finance.domain.movement.vo;

import java.math.BigDecimal;

/**
 * Review data of a movement created from a bank statement.
 *
 * @param confidence classifier confidence (0 to 1)
 * @param needsReview {@code true} when the user should check the category
 */
public record MovementReviewVO(BigDecimal confidence, boolean needsReview) {}

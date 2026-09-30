package com.jbh.finance.application.feature.statement.dto;

import java.math.BigDecimal;

/**
 * The category the classifier picked for one question.
 *
 * @param key key of the question
 * @param alias picked category alias
 * @param confidence value between 0 and 1
 */
public record ClassificationAnswerDTO(String key, String alias, BigDecimal confidence) {}

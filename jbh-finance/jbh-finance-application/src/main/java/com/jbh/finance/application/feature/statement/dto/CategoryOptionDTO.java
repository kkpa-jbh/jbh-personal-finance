package com.jbh.finance.application.feature.statement.dto;

/**
 * One category the classifier may pick.
 *
 * @param alias category alias returned as the answer
 * @param rule text the classifier reads to decide
 */
public record CategoryOptionDTO(String alias, String rule) {}

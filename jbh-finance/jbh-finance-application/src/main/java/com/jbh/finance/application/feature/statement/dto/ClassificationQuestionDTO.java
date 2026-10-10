package com.jbh.finance.application.feature.statement.dto;

import java.util.List;

/**
 * Asks the classifier to pick one category option for a movement.
 *
 * @param key unique key of the question inside a batch
 * @param description movement text
 * @param direction money in or money out
 * @param options the categories allowed for this direction
 */
public record ClassificationQuestionDTO(
    String key,
    String description,
    StatementMovementDirection direction,
    List<CategoryOptionDTO> options) {}

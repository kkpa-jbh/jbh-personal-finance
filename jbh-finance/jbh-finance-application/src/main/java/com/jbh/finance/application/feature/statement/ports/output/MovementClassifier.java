package com.jbh.finance.application.feature.statement.ports.output;

import com.jbh.commons.exception.BusinessException;
import com.jbh.finance.application.feature.statement.dto.ClassificationAnswerDTO;
import com.jbh.finance.application.feature.statement.dto.ClassificationQuestionDTO;
import java.util.List;

/** Picks one category option per question. Called once per batch. */
public interface MovementClassifier {
  List<ClassificationAnswerDTO> classify(List<ClassificationQuestionDTO> questions)
      throws BusinessException;
}

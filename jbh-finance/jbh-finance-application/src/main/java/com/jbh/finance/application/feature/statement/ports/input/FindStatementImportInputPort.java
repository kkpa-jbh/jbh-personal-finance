package com.jbh.finance.application.feature.statement.ports.input;

import com.jbh.commons.exception.BusinessException;
import com.jbh.finance.application.feature.movement.ports.output.MovementQueryRepository;
import com.jbh.finance.application.feature.product.services.ProductLifecycleService;
import com.jbh.finance.application.feature.statement.commands.FindStatementImportCommand;
import com.jbh.finance.application.feature.statement.dto.StatementImportResultDTO;
import com.jbh.finance.application.feature.statement.dto.StatementImportSettings;
import com.jbh.finance.application.feature.statement.ports.output.StatementImportRepository;
import com.jbh.finance.application.feature.statement.usecases.FindStatementImportUseCase;
import java.time.Clock;

public class FindStatementImportInputPort implements FindStatementImportUseCase {

  private final ProductLifecycleService productService;
  private final StatementImportRepository importRepository;
  private final MovementQueryRepository movementQueryRepository;
  private final StatementImportSettings settings;
  private final Clock clock;

  public FindStatementImportInputPort(
      final ProductLifecycleService productService,
      final StatementImportRepository importRepository,
      final MovementQueryRepository movementQueryRepository,
      final StatementImportSettings settings,
      final Clock clock) {
    this.productService = productService;
    this.importRepository = importRepository;
    this.movementQueryRepository = movementQueryRepository;
    this.settings = settings;
    this.clock = clock;
  }

  @Override
  public StatementImportResultDTO find(final FindStatementImportCommand command)
      throws BusinessException {
    throw new UnsupportedOperationException("not implemented");
  }
}

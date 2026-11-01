package com.jbh.finance.application.feature.statement.ports.input;

import com.jbh.commons.exception.BusinessException;
import com.jbh.finance.application.feature.product.services.ProductLifecycleService;
import com.jbh.finance.application.feature.statement.commands.StartStatementImportCommand;
import com.jbh.finance.application.feature.statement.dto.StatementImportSettings;
import com.jbh.finance.application.feature.statement.ports.output.StatementImportRepository;
import com.jbh.finance.application.feature.statement.usecases.StartStatementImportUseCase;
import java.util.UUID;

public class StartStatementImportInputPort implements StartStatementImportUseCase {

  private final ProductLifecycleService productService;
  private final StatementImportRepository importRepository;
  private final StatementImportSettings settings;

  public StartStatementImportInputPort(
      final ProductLifecycleService productService,
      final StatementImportRepository importRepository,
      final StatementImportSettings settings) {
    this.productService = productService;
    this.importRepository = importRepository;
    this.settings = settings;
  }

  @Override
  public UUID start(final StartStatementImportCommand command) throws BusinessException {
    throw new UnsupportedOperationException("not implemented");
  }
}

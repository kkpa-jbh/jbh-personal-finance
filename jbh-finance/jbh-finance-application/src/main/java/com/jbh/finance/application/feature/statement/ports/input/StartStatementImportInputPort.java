package com.jbh.finance.application.feature.statement.ports.input;

import com.jbh.commons.exception.BusinessException;
import com.jbh.commons.exception.GenericSpecificationException;
import com.jbh.finance.application.feature.product.dto.ProductDTO;
import com.jbh.finance.application.feature.product.services.ProductLifecycleService;
import com.jbh.finance.application.feature.statement.commands.StartStatementImportCommand;
import com.jbh.finance.application.feature.statement.dto.NewStatementImportDTO;
import com.jbh.finance.application.feature.statement.dto.StatementImportSettings;
import com.jbh.finance.application.feature.statement.ports.output.StatementImportRepository;
import com.jbh.finance.application.feature.statement.usecases.StartStatementImportUseCase;
import com.jbh.finance.application.shared.exceptions.BusinessApplicationExceptionType;
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
    if (command == null) {
      throw new GenericSpecificationException("Command cannot be null");
    }
    final ProductDTO product =
        productService.findOrThrowByUserAndProductId(command.userId(), command.productId());
    if (!product.type().allowsStatementImport()) {
      throw new BusinessException(
          BusinessApplicationExceptionType.STATEMENT_PRODUCT_TYPE_NOT_ALLOWED);
    }

    importRepository.markStaleAsFailed(
        command.productId(), command.pdfSha256(), settings.staleAfter());
    final UUID importId = UUID.randomUUID();
    final boolean inserted =
        importRepository.insertProcessing(
            new NewStatementImportDTO(
                importId, command.userId(), command.productId(), command.pdfSha256()));
    if (!inserted) {
      throw new BusinessException(BusinessApplicationExceptionType.STATEMENT_ALREADY_IMPORTED);
    }
    return importId;
  }
}

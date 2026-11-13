package com.jbh.finance.application.feature.movement.ports.input;

import com.jbh.commons.exception.BusinessException;
import com.jbh.finance.application.acid.UnitOfWork;
import com.jbh.finance.application.common.logging.LoggerFactory;
import com.jbh.finance.application.feature.category.dto.CategoryDTO;
import com.jbh.finance.application.feature.category.services.CategoryService;
import com.jbh.finance.application.feature.monthlybalance.dto.MonthlyBalanceDTO;
import com.jbh.finance.application.feature.monthlybalance.services.MonthlyBalanceSyncForUploadedMovements;
import com.jbh.finance.application.feature.movement.commands.AddMovementUploadedFileCommand;
import com.jbh.finance.application.feature.movement.commands.UploadMovementsBatchCommand;
import com.jbh.finance.application.feature.movement.dto.AddMultipleBasicMovementDTO;
import com.jbh.finance.application.feature.movement.mappers.CategoryMapper;
import com.jbh.finance.application.feature.movement.mappers.MovementMapper;
import com.jbh.finance.application.feature.movement.ports.output.MovementWriterRepository;
import com.jbh.finance.application.feature.movement.usecases.AddMovementsUploadedFileUseCase;
import com.jbh.finance.application.feature.product.dto.ProductDTO;
import com.jbh.finance.application.feature.product.services.ProductLifecycleService;
import com.jbh.finance.domain.movement.MovementDomain;
import com.jbh.finance.domain.movement.vo.MovementId;
import com.jbh.finance.domain.movement.vo.MovementMetadata;
import com.jbh.finance.domain.movement.vo.MovementType;
import com.jbh.finance.domain.product.ProductDomain;
import com.jbh.finance.domain.product.vo.ProductId;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import org.slf4j.Logger;

@SuppressWarnings("PMD.AvoidThrowingRawExceptionTypes")
public class AddMovementsUploadedFileInputPort implements AddMovementsUploadedFileUseCase {
  private static final Logger LOG =
      LoggerFactory.getLogger(AddMovementsUploadedFileInputPort.class);

  private final MovementWriterRepository movementRepo;
  private final ProductLifecycleService accountService;
  private final MonthlyBalanceSyncForUploadedMovements monthlyBalanceSyncerService;
  private final UnitOfWork unitOfWork;
  private final CategoryService categoryService;

  public AddMovementsUploadedFileInputPort(
      final ProductLifecycleService accountService,
      final MovementWriterRepository movementRepo,
      final UnitOfWork unitOfWork,
      final MonthlyBalanceSyncForUploadedMovements monthlyBalanceSyncerService,
      final CategoryService categoryService) {
    this.movementRepo = movementRepo;
    this.accountService = accountService;
    this.unitOfWork = unitOfWork;
    this.monthlyBalanceSyncerService = monthlyBalanceSyncerService;
    this.categoryService = categoryService;
  }

  /**
   * The user adds a list of basic movements to the productDTO by uploading a CSV file. \n The
   * movements are validated and persisted in the database. The monthly balance is also synced
   * asynchronously.
   *
   * @param userId
   * @param accountId
   * @param allUploadedMovCommand
   * @return
   */
  @Override
  public AddMultipleBasicMovementDTO uploadMovementsFromFile(
      final UUID userId,
      final ProductId accountId,
      final List<AddMovementUploadedFileCommand> allUploadedMovCommand)
      throws BusinessException {

    return uploadMovementsFromFile(
        userId, accountId, UploadMovementsBatchCommand.forFile(allUploadedMovCommand));
  }

  @Override
  public AddMultipleBasicMovementDTO uploadMovementsFromFile(
      final UUID userId, final ProductId accountId, final UploadMovementsBatchCommand batch)
      throws BusinessException {
    validateUploadedMovements(batch.movements());

    return unitOfWork.executeInTransaction(() -> uploadMovementsLocked(userId, accountId, batch));
  }

  /**
   * Runs in one transaction. The product row lock is the first statement, so other requests for
   * this product, on any instance, wait until the movements and the monthly balances are saved.
   */
  private AddMultipleBasicMovementDTO uploadMovementsLocked(
      final UUID userId,
      final ProductId accountId,
      final UploadMovementsBatchCommand batch)
      throws BusinessException {
    accountService.lockForUpdate(accountId);

    final ProductDomain accountDomain = findAccountOrElseThrow(userId, accountId);

    final List<MovementDomain> uploadedMovements =
        mapCommandToDomain(batch.movements(), accountDomain);

    // Sync productDTO balance
    final ProductDTO accountDTO =
        accountService.syncByUploadedMovements(
            accountDomain, uploadedMovements, batch.options());

    LOG.info("Persisting Movements changes");
    movementRepo.save(uploadedMovements.stream().map(MovementMapper::toDTO).toList());
    LOG.info("Saving productDTO changes");
    accountService.save(accountDTO);

    // Saves the monthly balances now and adjusts them in the background
    final List<MonthlyBalanceDTO> monthlyBalancesPersisted =
        monthlyBalanceSyncerService.syncForUploadedMovementsAsync(accountId, uploadedMovements);

    return new AddMultipleBasicMovementDTO(accountDTO, monthlyBalancesPersisted);
  }

  private static void validateUploadedMovements(
      final List<AddMovementUploadedFileCommand> allUploadedMovCommand) {
    if (allUploadedMovCommand == null || allUploadedMovCommand.isEmpty()) {
      throw new IllegalArgumentException("Movement list cannot be null or empty");
    }

    // Loop through all movements to validate using Index for better error tracing
    for (int i = 0; i < allUploadedMovCommand.size(); i++) {
      try {
        allUploadedMovCommand.get(i).validate();
      } catch (final IllegalArgumentException e) {
        throw new IllegalArgumentException(
            "Validation failed for movement at index " + i + ": " + e.getMessage(), e);
      }
    }
  }

  private ProductDomain findAccountOrElseThrow(final UUID userId, final ProductId accountId)
      throws BusinessException {
    final ProductDTO accountDTO = accountService.findOrThrowByUserAndProductId(userId, accountId);

    return accountDTO.toDomain();
  }

  private List<MovementDomain> mapCommandToDomain(
      final List<AddMovementUploadedFileCommand> allSimpleMovements,
      final ProductDomain accountDomain) {
    final var importedAt = LocalDateTime.now();
    return allSimpleMovements.stream()
        .map(command -> toMovementDomain(command, accountDomain, importedAt))
        .toList();
  }

  private MovementDomain toMovementDomain(
      final AddMovementUploadedFileCommand command,
      final ProductDomain accountDomain,
      final LocalDateTime importedAt) {
    try {
      final var mov =
          new MovementDomain(
              MovementId.generate(),
              accountDomain.getId(),
              command.movementType(),
              CategoryMapper.toDomain(resolveCategory(command)),
              command.totalAmount(),
              command.entryDate(),
              command.balanceSnapshot(),
              MovementMetadata.createEmpty(),
              command.description(),
              command.statementImportId());

      mov.validate();
      mov.getMetadata().putFileImportedAt(importedAt);
      if (command.review() != null) {
        mov.getMetadata().putStatementReview(command.review());
      }

      return mov;

    } catch (final BusinessException e) {
      throw new RuntimeException(e);
    }
  }

  private CategoryDTO resolveCategory(final AddMovementUploadedFileCommand command) {
    if (command.category() != null) {
      return command.category();
    }
    if (command.movementType() == MovementType.DEPOSIT) {
      return getOtherIncomeCategory();
    }
    if (command.movementType() == MovementType.WITHDRAWAL) {
      return getUnknownExpenseCategory();
    }
    return null;
  }

  private CategoryDTO getOtherIncomeCategory() {
    return categoryService.findIncomeOther();
  }

  private CategoryDTO getUnknownExpenseCategory() {
    return categoryService.findExpenseUnknown();
  }
}

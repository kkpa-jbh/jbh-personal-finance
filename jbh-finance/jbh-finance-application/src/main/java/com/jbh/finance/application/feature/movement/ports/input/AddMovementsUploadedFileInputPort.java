package com.jbh.finance.application.feature.movement.ports.input;

import com.jbh.commons.exception.BusinessException;
import com.jbh.finance.application.acid.UnitOfWork;
import com.jbh.finance.application.common.logging.LoggerFactory;
import com.jbh.finance.application.feature.category.dto.CategoryDTO;
import com.jbh.finance.application.feature.category.services.CategoryService;
import com.jbh.finance.application.feature.monthlybalance.dto.MonthlyBalanceDTO;
import com.jbh.finance.application.feature.monthlybalance.services.MonthlyBalanceSyncForUploadedMovements;
import com.jbh.finance.application.feature.movement.commands.AddMovementUploadedFileCommand;
import com.jbh.finance.application.feature.movement.dto.AddMultipleBasicMovementDTO;
import com.jbh.finance.application.feature.movement.mappers.CategoryMapper;
import com.jbh.finance.application.feature.movement.mappers.MovementMapper;
import com.jbh.finance.application.feature.movement.ports.output.MovementWriterRepository;
import com.jbh.finance.application.feature.movement.usecases.AddMovementsUploadedFileUseCase;
import com.jbh.finance.application.feature.product.dto.ProductDTO;
import com.jbh.finance.application.feature.product.services.ProductLifecycleService;
import com.jbh.finance.domain.movement.MovementDomain;
import com.jbh.finance.domain.movement.vo.MovementMetadata;
import com.jbh.finance.domain.movement.vo.MovementType;
import com.jbh.finance.domain.product.ProductDomain;
import com.jbh.finance.domain.product.vo.ProductId;
import java.math.BigDecimal;
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

    validateUploadedMovements(allUploadedMovCommand);

    final ProductDomain accountDomain = findAccountOrElseThrow(userId, accountId);

    final List<MovementDomain> uploadedMovements =
        mapCommandToDomain(allUploadedMovCommand, accountDomain);

    // Sync productDTO balance
    final ProductDTO accountDTO =
        accountService.syncByUploadedMovements(accountDomain, uploadedMovements);

    // Persist Movements and Account UOW
    persistMovementAndAccountUOW(uploadedMovements, accountDTO);

    // Sync monthly Balance Asynchronously
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
        .map(
            mvmntCommand -> {
              final BigDecimal totalAmount = mvmntCommand.totalAmount();
              final MovementType movementType = mvmntCommand.movementType();
              CategoryDTO categoryDTO = null;
              if (movementType == MovementType.DEPOSIT) {
                categoryDTO = getOtherIncomeCategory();
              } else if (movementType == MovementType.WITHDRAWAL) {
                categoryDTO = getUnknownExpenseCategory();
              }

              try {
                final var mov =
                    new MovementDomain(
                        accountDomain.getId(),
                        movementType,
                        mvmntCommand.entryDate(),
                        totalAmount,
                        mvmntCommand.balanceSnapshot(),
                        MovementMetadata.createEmpty(),
                        CategoryMapper.toDomain(categoryDTO),
                        null);

                mov.validate();
                mov.getMetadata().putFileImportedAt(importedAt);

                return mov;

              } catch (final BusinessException e) {
                throw new RuntimeException(e);
              }
            })
        .toList();
  }

  private void persistMovementAndAccountUOW(
      final List<MovementDomain> newMovements, final ProductDTO accountDTO) {
    unitOfWork.execute(
        () -> {
          LOG.info("Persisting Movements changes");
          movementRepo.save(newMovements.stream().map(MovementMapper::toDTO).toList());
          LOG.info("Saving productDTO changes");
          accountService.save(accountDTO);
        });
  }

  private CategoryDTO getOtherIncomeCategory() {
    return categoryService.findIncomeOther();
  }

  private CategoryDTO getUnknownExpenseCategory() {
    return categoryService.findExpenseUnknown();
  }
}

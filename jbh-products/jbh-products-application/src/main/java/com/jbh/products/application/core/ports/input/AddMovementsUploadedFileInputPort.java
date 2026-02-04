package com.jbh.products.application.core.ports.input;

import com.jbh.commons.exception.BusinessException;
import com.jbh.products.application.acid.UnitOfWork;
import com.jbh.products.application.common.logging.LoggerFactory;
import com.jbh.products.application.core.dto.AddMultipleBasicMovementDTO;
import com.jbh.products.application.core.dto.MonthlyBalanceDTO;
import com.jbh.products.application.core.dto.ProductDTO;
import com.jbh.products.application.core.mappers.MovementMapper;
import com.jbh.products.application.core.services.MonthlyBalanceSyncForUploadedMovements;
import com.jbh.products.application.core.services.account.ProductsService;
import com.jbh.products.application.core.usecases.AddMovementsUploadedFileUseCase;
import com.jbh.products.application.core.vo.commands.AddMovementUploadedFileCommand;
import com.jbh.products.application.movements.ports.output.AccountMovementWriterRepository;
import com.jbh.products.domain.entity.MovementDomain;
import com.jbh.products.domain.entity.ProductDomain;
import com.jbh.products.domain.vo.ProductId;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import org.slf4j.Logger;

@SuppressWarnings("PMD.AvoidThrowingRawExceptionTypes")
public class AddMovementsUploadedFileInputPort implements AddMovementsUploadedFileUseCase {
  private static final Logger LOG =
      LoggerFactory.getLogger(AddMovementsUploadedFileInputPort.class);

  private final AccountMovementWriterRepository movementRepo;
  private final ProductsService accountService;
  private final MonthlyBalanceSyncForUploadedMovements monthlyBalanceSyncerService;
  private final UnitOfWork unitOfWork;

  public AddMovementsUploadedFileInputPort(
      final ProductsService accountService,
      final AccountMovementWriterRepository movementRepo,
      final UnitOfWork unitOfWork,
      final MonthlyBalanceSyncForUploadedMovements monthlyBalanceSyncerService) {
    this.movementRepo = movementRepo;
    this.accountService = accountService;
    this.unitOfWork = unitOfWork;
    this.monthlyBalanceSyncerService = monthlyBalanceSyncerService;
  }

  /**
   * The user adds a list of basic movements to the account by uploading a CSV file. \n The
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

    // Sync account balance
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
    final ProductDTO accountDTO = accountService.findByUserAndProductId(userId, accountId);

    return accountDTO.toDomain();
  }

  private List<MovementDomain> mapCommandToDomain(
      final List<AddMovementUploadedFileCommand> allSimpleMovements,
      final ProductDomain accountDomain) {
    return allSimpleMovements.stream()
        .map(
            mvmntCommand -> {
              final BigDecimal totalAmount = mvmntCommand.totalAmount();

              try {
                return MovementDomain.withFileImport(
                    accountDomain.getId(),
                    mvmntCommand.entryDate(),
                    totalAmount,
                    mvmntCommand.balanceSnapshot(),
                    mvmntCommand.movementType(),
                    LocalDateTime.now());
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
          LOG.info("Saving account changes");
          accountService.save(accountDTO);
        });
  }
}

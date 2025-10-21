package com.jbh.account.application.core.ports.input;

import static com.jbh.account.application.core.mappers.AccountMapper.toDomain;

import com.jbh.account.application.acid.UnitOfWork;
import com.jbh.account.application.common.logging.LoggerFactory;
import com.jbh.account.application.core.dto.AccountDTO;
import com.jbh.account.application.core.dto.AddMultipleBasicMovementDTO;
import com.jbh.account.application.core.dto.MonthlyBalanceDTO;
import com.jbh.account.application.core.mappers.MovementMapper;
import com.jbh.account.application.core.services.MonthlyBalanceSyncForUploadedMovements;
import com.jbh.account.application.core.services.account.AccountService;
import com.jbh.account.application.core.usecases.AddMovementsUploadedFileUseCase;
import com.jbh.account.application.core.vo.commands.AddMovementUploadedFileCommand;
import com.jbh.account.application.movements.ports.output.AccountMovementRepository;
import com.jbh.account.domain.entity.AccountDomain;
import com.jbh.account.domain.entity.AccountMovementDomain;
import com.jbh.account.domain.exceptions.AccountBusinessException;
import com.jbh.account.domain.vo.AccountId;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import org.slf4j.Logger;

@SuppressWarnings("PMD.AvoidThrowingRawExceptionTypes")
public class AddMovementsUploadedFileInputPort implements AddMovementsUploadedFileUseCase {
  private static final Logger LOG =
      LoggerFactory.getLogger(AddMovementsUploadedFileInputPort.class);

  private final AccountMovementRepository movementRepo;
  private final AccountService accountService;
  private final MonthlyBalanceSyncForUploadedMovements monthlyBalanceSyncerService;
  private final UnitOfWork unitOfWork;

  public AddMovementsUploadedFileInputPort(
      final AccountService accountService,
      final AccountMovementRepository movementRepo,
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
      final AccountId accountId,
      final List<AddMovementUploadedFileCommand> allUploadedMovCommand)
      throws AccountBusinessException {

    validateUploadedMovements(allUploadedMovCommand);

    final AccountDomain accountDomain = findAccountOrElseThrow(userId, accountId);

    final List<AccountMovementDomain> uploadedMovements =
        mapCommandToDomain(allUploadedMovCommand, accountDomain);

    // Sync account balance
    final AccountDTO accountDTO =
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

  private AccountDomain findAccountOrElseThrow(final UUID userId, final AccountId accountId) {
    if (userId == null) {
      LOG.error("User ID cannot be null");
      throw new IllegalArgumentException("User ID cannot be null");
    }

    final AccountDTO accountDTO =
        accountService
            .findByUserAndAccountId(userId, accountId)
            .orElseThrow(
                () -> {
                  LOG.error(
                      "Account not found for user: {} and account: {}", userId, accountId.value());
                  return new IllegalArgumentException("Account not found");
                });

    return toDomain(accountDTO);
  }

  private List<AccountMovementDomain> mapCommandToDomain(
      final List<AddMovementUploadedFileCommand> allSimpleMovements,
      final AccountDomain accountDomain) {
    return allSimpleMovements.stream()
        .map(
            mvmntCommand -> {
              final BigDecimal totalAmount = mvmntCommand.totalAmount();

              try {
                return AccountMovementDomain.withFileImport(
                    accountDomain.getId(),
                    mvmntCommand.entryDate(),
                    totalAmount,
                    mvmntCommand.balanceSnapshot(),
                    mvmntCommand.movementType(),
                    LocalDateTime.now());
              } catch (final AccountBusinessException e) {
                throw new RuntimeException(e);
              }
            })
        .toList();
  }

  private void persistMovementAndAccountUOW(
      final List<AccountMovementDomain> newMovements, final AccountDTO accountDTO) {
    unitOfWork.execute(
        () -> {
          LOG.info("Persisting Movements changes");
          movementRepo.save(newMovements.stream().map(MovementMapper::toDTO).toList());
          LOG.info("Saving account changes");
          accountService.save(accountDTO);
        });
  }
}

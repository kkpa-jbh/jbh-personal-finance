package com.jbh.account.application.accounts.ports.input;

import static com.jbh.account.application.accounts.mappers.AccountMapper.toDTO;
import static com.jbh.account.application.accounts.mappers.AccountMapper.toDomain;

import com.jbh.account.application.accounts.dto.AccountDTO;
import com.jbh.account.application.accounts.dto.AddMultipleBasicMovementDTO;
import com.jbh.account.application.accounts.mappers.MovementMapper;
import com.jbh.account.application.accounts.ports.output.AccountRepository;
import com.jbh.account.application.accounts.services.MonthlyBalanceSyncerAppService;
import com.jbh.account.application.accounts.usecases.AddMovementsUploadedFileUseCase;
import com.jbh.account.application.accounts.vo.commands.AddMovementUploadedFileCommand;
import com.jbh.account.application.acid.UnitOfWork;
import com.jbh.account.application.common.logging.LoggerFactory;
import com.jbh.account.application.movements.ports.output.AccountMovementRepository;
import com.jbh.account.domain.entity.AccountDomain;
import com.jbh.account.domain.entity.AccountMonthlyBalanceDomain;
import com.jbh.account.domain.entity.AccountMovementDomain;
import com.jbh.account.domain.vo.AccountId;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import org.slf4j.Logger;

public class AddMovementsUploadedFileInputPort implements AddMovementsUploadedFileUseCase {
  private static final Logger LOG =
      LoggerFactory.getLogger(AddMovementsUploadedFileInputPort.class);

  private final AccountMovementRepository movementRepo;
  private final AccountRepository accountRepo;
  private final MonthlyBalanceSyncerAppService monthlyBalanceSyncerService;
  private final UnitOfWork unitOfWork;

  public AddMovementsUploadedFileInputPort(
      final AccountRepository accountRepo,
      final AccountMovementRepository movementRepo,
      final UnitOfWork unitOfWork,
      final MonthlyBalanceSyncerAppService monthlyBalanceSyncerService) {
    this.movementRepo = movementRepo;
    this.accountRepo = accountRepo;
    this.unitOfWork = unitOfWork;
    this.monthlyBalanceSyncerService = monthlyBalanceSyncerService;
  }

  /**
   * The user adds a list of basic movements to the account by uploading a CSV file. \n The
   * movements are validated and persisted in the database.
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

    final AccountDomain accountDomain = findAccount(userId, accountId);
    final List<AccountMovementDomain> multipleMovementsDomain =
        mapCommandToDomain(allUploadedMovCommand, accountDomain);

    final AccountDTO accountDTO =
        syncAccountBalanceByMovements(accountDomain, multipleMovementsDomain);
    persistMovement(multipleMovementsDomain, accountDTO);

    final List<AccountMonthlyBalanceDomain> monthlyBalancesPersisted =
        monthlyBalanceSyncerService.syncForUploadedMovementsAsync(
            accountId, multipleMovementsDomain);

    return new AddMultipleBasicMovementDTO(
        accountDTO,
        monthlyBalancesPersisted.stream().map(AccountMonthlyBalanceDomain::toDTO).toList());
  }

  private AccountDomain findAccount(final UUID userId, final AccountId accountId) {
    if (userId == null) {
      LOG.error("User ID cannot be null");
      throw new IllegalArgumentException("User ID cannot be null");
    }

    final AccountDTO accountDTO =
        accountRepo
            .findByAccountId(userId, accountId)
            .orElseThrow(
                () -> {
                  LOG.error(
                      "Account not found for user: {} and account: {}", userId, accountId.value());
                  return new IllegalArgumentException("Account not found");
                });

    return toDomain(accountDTO);
  }

  /**
   * Map the command to a domain object.
   *
   * <p>Domain object does not accept negative values for totalAmount. We need to make the
   * transformation here
   *
   * @param allSimpleMovements
   * @param accountDomain
   * @return
   */
  private List<AccountMovementDomain> mapCommandToDomain(
      final List<AddMovementUploadedFileCommand> allSimpleMovements,
      final AccountDomain accountDomain) {
    return allSimpleMovements.stream()
        .map(
            mvmntCommand -> {
              // Transform negative values to positive because the domain object does not accept
              // negative values
              final BigDecimal totalAmount = mvmntCommand.totalAmount();

              return AccountMovementDomain.withFileImport(
                  accountDomain.getId(),
                  mvmntCommand.entryDate(),
                  totalAmount,
                  mvmntCommand.balanceSnapshot(),
                  mvmntCommand.movementType(),
                  LocalDateTime.now());
            })
        .toList();
  }

  private AccountDTO syncAccountBalanceByMovements(
      final AccountDomain accountDomain, final List<AccountMovementDomain> newMovements) {
    accountDomain.syncBalancesWithUploadedMovements(newMovements);
    return toDTO(accountDomain);
  }

  private void persistMovement(
      final List<AccountMovementDomain> newMovements, final AccountDTO accountDomain) {
    unitOfWork.execute(
        () -> {
          LOG.info("Persisting Movements and account changes");
          movementRepo.save(newMovements.stream().map(MovementMapper::toDTO).toList());
          accountRepo.save(accountDomain);
        });
  }
}

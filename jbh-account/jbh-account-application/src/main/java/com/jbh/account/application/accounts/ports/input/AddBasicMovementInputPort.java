package com.jbh.account.application.accounts.ports.input;

import com.jbh.account.application.accounts.dto.AddBasicMovementDTO;
import com.jbh.account.application.accounts.dto.AddMultipleBasicMovementDTO;
import com.jbh.account.application.accounts.ports.output.AccountRepository;
import com.jbh.account.application.accounts.services.MonthlyBalanceSyncerAppService;
import com.jbh.account.application.accounts.usecases.AddMovementUseCase;
import com.jbh.account.application.accounts.vo.AddBasicMovementRequest;
import com.jbh.account.application.acid.UnitOfWork;
import com.jbh.account.application.common.logging.LoggerFactory;
import com.jbh.account.application.common.logging.LoggingContext;
import com.jbh.account.application.movements.ports.output.AccountMovementRepository;
import com.jbh.account.domain.entity.AccountDomain;
import com.jbh.account.domain.entity.AccountMonthlyBalanceDomain;
import com.jbh.account.domain.entity.AccountMovementDomain;
import com.jbh.account.domain.vo.AccountDomainDTO;
import com.jbh.account.domain.vo.AccountId;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import org.slf4j.Logger;

public class AddBasicMovementInputPort implements AddMovementUseCase {

  private static final Logger LOG = LoggerFactory.getLogger(AddBasicMovementInputPort.class);
  private final AccountMovementRepository movementRepo;
  private final AccountRepository accountRepo;
  private final MonthlyBalanceSyncerAppService monthlyBalanceSyncerService;
  private final UnitOfWork unitOfWork;

  public AddBasicMovementInputPort(
      final AccountRepository accountRepo,
      final AccountMovementRepository movementRepo,
      final UnitOfWork unitOfWork,
      final MonthlyBalanceSyncerAppService monthlyBalanceSyncerService) {
    this.movementRepo = movementRepo;
    this.accountRepo = accountRepo;
    this.unitOfWork = unitOfWork;
    this.monthlyBalanceSyncerService = monthlyBalanceSyncerService;
  }

  @Override
  public AddBasicMovementDTO addBasicMovements(
      final UUID userId,
      final AccountId accountId,
      final AddBasicMovementRequest basicMovementRequest) {
    return LoggingContext.builder()
        .accountId(accountId.value())
        .userId(userId)
        .module("account-application")
        .execute(
            () -> {
              LOG.info(
                  "Starting Movement addition for account: {}, amount: {} snapshot: {}",
                  accountId.value(),
                  basicMovementRequest.totalAmount(),
                  basicMovementRequest.balanceSnapshot());

              // Input validations
              basicMovementRequest.validate();

              // Sync account balance
              final AccountDomain accountDomain = findAccount(userId, accountId);
              final AccountMovementDomain newMovement =
                  AccountMovementDomain.withBalances(
                      accountDomain.getId(),
                      basicMovementRequest.entryDate(),
                      basicMovementRequest.totalAmount(),
                      basicMovementRequest.balanceSnapshot());
              final AccountDomainDTO accountDTO =
                  syncAccountBalanceByMovements(accountDomain, newMovement);
              persistMovement(newMovement, accountDTO);

              // Sync monthly balance
              final AccountMonthlyBalanceDomain accountMonthlyBalance =
                  monthlyBalanceSyncerService.syncMonthlyBalanceAsync(newMovement);

              LOG.info(
                  "Movement addition completed successfully for account: {}", accountId.value());

              return new AddBasicMovementDTO(
                  accountDTO, accountMonthlyBalance, newMovement.toDTO());
            });
  }

  @Override
  public AddMultipleBasicMovementDTO addBasicMovements(
      final UUID userId,
      final AccountId accountId,
      final List<AddBasicMovementRequest> allSimpleMovements) {

    if (allSimpleMovements == null || allSimpleMovements.isEmpty()) {
      throw new IllegalArgumentException("Movement list cannot be null or empty");
    }

    // Loop through all movements to validate using Index for better error tracing
    for (int i = 0; i < allSimpleMovements.size(); i++) {
      try {
        allSimpleMovements.get(i).validate();
      } catch (final IllegalArgumentException e) {
        throw new IllegalArgumentException(
            "Validation failed for movement at index " + i + ": " + e.getMessage(), e);
      }
    }

    final AccountDomain accountDomain = findAccount(userId, accountId);
    final List<AccountMovementDomain> multipleMovementsDomain =
        mapSimpleMovementsToDomain(allSimpleMovements, accountDomain);
    final AccountDomainDTO accountDTO =
        syncAccountBalanceByMovements(accountDomain, multipleMovementsDomain);
    persistMovement(multipleMovementsDomain, accountDTO);

    final List<AccountMonthlyBalanceDomain> monthlyBalancesPersisted =
        monthlyBalanceSyncerService.syncMonthlyBalanceAsync(accountId, multipleMovementsDomain);

    return new AddMultipleBasicMovementDTO(
        accountDTO,
        monthlyBalancesPersisted.stream().map(AccountMonthlyBalanceDomain::toDTO).toList());
  }

  private List<AccountMovementDomain> mapSimpleMovementsToDomain(
      final List<AddBasicMovementRequest> allSimpleMovements, final AccountDomain accountDomain) {
    return allSimpleMovements.stream()
        .map(
            movementRequest ->
                AccountMovementDomain.withFileImport(
                    accountDomain.getId(),
                    movementRequest.entryDate(),
                    movementRequest.totalAmount(),
                    movementRequest.balanceSnapshot(),
                    LocalDateTime.now()))
        .toList();
  }

  private AccountDomainDTO syncAccountBalanceByMovements(
      final AccountDomain accountDomain, final List<AccountMovementDomain> newMovements) {
    accountDomain.syncBalances(newMovements);
    return accountDomain.toDTO();
  }

  private void persistMovement(
      final List<AccountMovementDomain> newMovements, final AccountDomainDTO accountDomain) {
    unitOfWork.execute(
        () -> {
          LOG.info("Persisting Movements and account changes");
          movementRepo.save(newMovements.stream().map(AccountMovementDomain::toDTO).toList());
          accountRepo.save(accountDomain);
        });
  }

  private AccountDomain findAccount(final UUID userId, final AccountId accountId) {
    if (userId == null) {
      LOG.error("User ID cannot be null");
      throw new IllegalArgumentException("User ID cannot be null");
    }

    return accountRepo
        .findByAccountId(userId, accountId)
        .orElseThrow(
            () -> {
              LOG.error(
                  "Account not found for user: {} and account: {}", userId, accountId.value());
              return new IllegalArgumentException("Account not found");
            });
  }

  private AccountDomainDTO syncAccountBalanceByMovements(
      final AccountDomain accountDomain, final AccountMovementDomain newMovement) {
    accountDomain.syncBalances(Collections.singletonList(newMovement));
    return accountDomain.toDTO();
  }

  private void persistMovement(
      final AccountMovementDomain newMovement, final AccountDomainDTO accountDomain) {
    unitOfWork.execute(
        () -> {
          LOG.info("Persisting 1 Movement and account changes");
          movementRepo.save(newMovement.toDTO());
          accountRepo.save(accountDomain);
        });
  }
}

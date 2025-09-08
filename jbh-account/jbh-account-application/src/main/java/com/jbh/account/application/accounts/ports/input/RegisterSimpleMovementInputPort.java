package com.jbh.account.application.accounts.ports.input;

import com.jbh.account.application.accounts.dto.AddBasicMovementDTO;
import com.jbh.account.application.accounts.dto.AddMultipleBasicMovementDTO;
import com.jbh.account.application.accounts.ports.output.AccountRepository;
import com.jbh.account.application.accounts.services.MonthlyBalanceSyncerAppService;
import com.jbh.account.application.accounts.usecases.RegisterMovementUseCase;
import com.jbh.account.application.accounts.vo.AddBasicMovementRequest;
import com.jbh.account.application.acid.UnitOfWork;
import com.jbh.account.application.common.logging.LoggerFactory;
import com.jbh.account.application.common.logging.LoggingContext;
import com.jbh.account.application.movements.ports.output.AccountMovementRepository;
import com.jbh.accounts_mgmt.accounts.AccountDomain;
import com.jbh.accounts_mgmt.accounts.AccountId;
import com.jbh.accounts_mgmt.accounts.AccountMonthlyBalanceDomain;
import com.jbh.accounts_mgmt.movements.AccountMovementDomain;
import java.util.List;
import java.util.UUID;
import org.slf4j.Logger;

public class RegisterSimpleMovementInputPort implements RegisterMovementUseCase {

  private static final Logger LOG = LoggerFactory.getLogger(RegisterSimpleMovementInputPort.class);
  private final AccountMovementRepository movementRepo;
  private final AccountRepository accountRepo;
  private final MonthlyBalanceSyncerAppService monthlyBalanceSyncerService;


  private final UnitOfWork unitOfWork;

  public RegisterSimpleMovementInputPort(AccountRepository accountRepo,
      AccountMovementRepository movementRepo,
      UnitOfWork unitOfWork,
      MonthlyBalanceSyncerAppService monthlyBalanceSyncerService) {
    this.movementRepo = movementRepo;
    this.accountRepo = accountRepo;
    this.unitOfWork = unitOfWork;
    this.monthlyBalanceSyncerService = monthlyBalanceSyncerService;
  }


  @Override
  public AddBasicMovementDTO addBasicMovements(UUID userId, AccountId accountId,
      AddBasicMovementRequest basicMovementRequest) {
    return LoggingContext.builder()
        .accountId(accountId.value())
        .userId(userId)
        .module("account-application")
        .execute(() -> {

          LOG.info("Starting Movement addition for account: {}, amount: {} snapshot: {}",
              accountId.value(), basicMovementRequest.totalAmount(), basicMovementRequest.balanceSnapshot());

          // Input validations
          basicMovementRequest.validate();

          // Sync account balance
          AccountDomain accountDomain = findAccount(userId, accountId);
          AccountMovementDomain newMovement = AccountMovementDomain.of(accountDomain.getId(),
              basicMovementRequest.entryDate(),
              basicMovementRequest.totalAmount(), basicMovementRequest.balanceSnapshot());
          syncAccountBalanceByMovements(accountDomain, newMovement);

          // Sync monthly balance
          AccountMonthlyBalanceDomain accountMonthlyBalance = monthlyBalanceSyncerService.syncMonthlyBalance(
              newMovement);

          LOG.info("Movement addition completed successfully for account: {}", accountId.value());

          return new AddBasicMovementDTO(accountDomain, accountMonthlyBalance, newMovement);
        });

  }

  private void syncAccountBalanceByMovements(AccountDomain accountDomain,
      AccountMovementDomain newMovement) {
    accountDomain.syncBalances(newMovement);
    LOG.debug("Account balance updated to: {}", accountDomain.getMovementBalance());
    persistMovements(newMovement, accountDomain);
  }

  private void syncAccountBalanceByMovements(AccountDomain accountDomain,
      List<AccountMovementDomain> newMovements) {
    accountDomain.syncBalances(newMovements);
    LOG.debug("Account balance updated to: {}", accountDomain.getMovementBalance());
    persistMovements(newMovements, accountDomain);
  }

  private void persistMovements(AccountMovementDomain newMovement, AccountDomain accountDomain) {
    unitOfWork.execute(() -> {
      LOG.info("Persisting Movement and account changes");
      movementRepo.save(newMovement);
      accountRepo.save(accountDomain);
    });
  }

  private void persistMovements(List<AccountMovementDomain> newMovements, AccountDomain accountDomain) {
    unitOfWork.execute(() -> {
      LOG.info("Persisting Movement and account changes");
      movementRepo.save(newMovements);
      accountRepo.save(accountDomain);
    });
  }

  private AccountDomain findAccount(UUID userId, AccountId accountId) {
    if (userId == null) {
      LOG.error("User ID cannot be null");
      throw new IllegalArgumentException("User ID cannot be null");
    }

    return accountRepo.findByAccountId(userId, accountId)
        .orElseThrow(() -> {
          LOG.error("Account not found for user: {} and account: {}",
              userId, accountId.value());
          return new IllegalArgumentException("Account not found");
        });
  }

  @Override
  public AddMultipleBasicMovementDTO addBasicMovements(UUID userId, AccountId accountId,
      List<AddBasicMovementRequest> allSimpleMovements) {

    if (allSimpleMovements == null || allSimpleMovements.isEmpty()) {
      throw new IllegalArgumentException("Movement list cannot be null or empty");
    }

    // Loop through all movements to validate using Index for better error tracing
    for (int i = 0; i < allSimpleMovements.size(); i++) {
      try {
        allSimpleMovements.get(i).validate();
      } catch (Exception e) {
        throw new IllegalArgumentException("Validation failed for movement at index " + i + ": " + e.getMessage());
      }
    }

    AccountDomain accountDomain = findAccount(userId, accountId);
    List<AccountMovementDomain> multipleMovementsDomain = mapSimpleMovementsToDomain(allSimpleMovements, accountDomain);
    syncAccountBalanceByMovements(accountDomain, multipleMovementsDomain);

    List<AccountMonthlyBalanceDomain> monthlyBalancesToPersist =
        monthlyBalanceSyncerService.syncMonthlyBalance(accountId, multipleMovementsDomain);

    return new AddMultipleBasicMovementDTO(accountDomain, monthlyBalancesToPersist);

  }

  private List<AccountMovementDomain> mapSimpleMovementsToDomain(List<AddBasicMovementRequest> allSimpleMovements,
      AccountDomain accountDomain) {
    return allSimpleMovements.stream().map(movementRequest -> AccountMovementDomain.of(
            accountDomain.getId(),
            movementRequest.entryDate(),
            movementRequest.totalAmount(),
            movementRequest.balanceSnapshot()))
        .toList();
  }


}

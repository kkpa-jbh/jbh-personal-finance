package com.jbh.account.application.core.ports.input;

import static com.jbh.account.application.core.mappers.MonthlyBalanceMapper.toDTO;

import com.jbh.account.application.acid.UnitOfWork;
import com.jbh.account.application.common.logging.LoggerFactory;
import com.jbh.account.application.common.logging.LoggingContext;
import com.jbh.account.application.core.dto.AccountDTO;
import com.jbh.account.application.core.dto.AccountMonthlyBalanceDTO;
import com.jbh.account.application.core.dto.AddBasicMovementDTO;
import com.jbh.account.application.core.mappers.AccountMapper;
import com.jbh.account.application.core.mappers.MovementMapper;
import com.jbh.account.application.core.services.MonthlyBalanceSyncerAppService;
import com.jbh.account.application.core.services.account.AccountService;
import com.jbh.account.application.core.usecases.AddMovementUseCase;
import com.jbh.account.application.core.vo.commands.AddMovementCommand;
import com.jbh.account.application.movements.ports.output.AccountMovementRepository;
import com.jbh.account.domain.entity.AccountDomain;
import com.jbh.account.domain.entity.AccountMonthlyBalanceDomain;
import com.jbh.account.domain.entity.AccountMovementDomain;
import com.jbh.account.domain.entity.MovementCategoryDomain;
import com.jbh.account.domain.vo.AccountId;
import com.jbh.account.domain.vo.MovementType;
import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;
import org.slf4j.Logger;

public class AddMovementInputPort implements AddMovementUseCase {

  private static final Logger LOG = LoggerFactory.getLogger(AddMovementInputPort.class);
  private final AccountMovementRepository movementRepo;
  private final AccountService accountService;
  private final MonthlyBalanceSyncerAppService monthlyBalanceSyncerService;
  private final UnitOfWork unitOfWork;

  public AddMovementInputPort(
      final AccountService accountService,
      final AccountMovementRepository movementRepo,
      final UnitOfWork unitOfWork,
      final MonthlyBalanceSyncerAppService monthlyBalanceSyncerService) {
    this.movementRepo = movementRepo;
    this.accountService = accountService;
    this.unitOfWork = unitOfWork;
    this.monthlyBalanceSyncerService = monthlyBalanceSyncerService;
  }

  @Override
  public AddBasicMovementDTO addMovement(
      final UUID userId, final AccountId accountId, final AddMovementCommand movementCommand) {
    return LoggingContext.builder()
        .accountId(accountId.value())
        .userId(userId)
        .module("account-application")
        .execute(
            () -> {

              // Input validations
              movementCommand.validate();

              LOG.info(
                  "Analyzing Movement {} for account: {}, date:{} amount: {} snapshot: {}",
                  movementCommand.movementType(),
                  accountId.value(),
                  movementCommand.entryDate(),
                  movementCommand.totalAmount(),
                  movementCommand.balanceSnapshot());

              // Sync account balance
              BigDecimal totalAmount = movementCommand.totalAmount();
              final MovementType movementType = movementCommand.movementType();
              totalAmount =
                  movementType == MovementType.WITHDRAWAL ? totalAmount.negate() : totalAmount;

              boolean wasOfficialReport = false;
              final Optional<AccountMonthlyBalanceDTO> existingMonthlyBalanceOpt =
                  monthlyBalanceSyncerService.findByAccountIdYearAndMonth(
                      accountId,
                      movementCommand.entryDate().getYear(),
                      movementCommand.entryDate().getMonthValue());
              if (existingMonthlyBalanceOpt.isPresent()) {
                wasOfficialReport = existingMonthlyBalanceOpt.get().officialMonthlyReport();
              }

              // Sync account balance
              final AccountDomain accountDomain = findAccount(userId, accountId);
              final AccountMovementDomain newMovement =
                  AccountMovementDomain.with(
                      accountDomain.getId(),
                      movementCommand.entryDate(),
                      totalAmount,
                      movementCommand.balanceSnapshot(),
                      movementType,
                      MovementCategoryDomain.withDTO(movementCommand.categoryDTO()));
              final AccountDTO accountDTO =
                  syncAccountBalanceByMovements(accountDomain, newMovement, wasOfficialReport);
              persistMovement(newMovement, accountDTO);

              // Sync monthly balance
              final AccountMonthlyBalanceDomain accountMonthlyBalance =
                  monthlyBalanceSyncerService.syncMonthlyBalanceAsync(newMovement);

              LOG.info(
                  "Movement addition completed successfully for account: {}", accountId.value());

              return new AddBasicMovementDTO(
                  accountDTO, toDTO(accountMonthlyBalance), MovementMapper.toDTO(newMovement));
            });
  }

  private AccountDomain findAccount(final UUID userId, final AccountId accountId) {
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

    return AccountMapper.toDomain(accountDTO);
  }

  private AccountDTO syncAccountBalanceByMovements(
      final AccountDomain accountDomain,
      final AccountMovementDomain newMovement,
      final boolean wasOfficialReport) {
    accountDomain.syncBalancesByMovement(newMovement, wasOfficialReport);
    return AccountMapper.toDTO(accountDomain);
  }

  private void persistMovement(
      final AccountMovementDomain newMovement, final AccountDTO accountDomain) {
    unitOfWork.execute(
        () -> {
          LOG.info("Persisting Movement {} and Synced Account", newMovement.getMovementDate());
          movementRepo.save(MovementMapper.toDTO(newMovement));
          accountService.save(accountDomain);
        });
  }
}

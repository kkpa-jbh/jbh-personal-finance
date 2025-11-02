package com.jbh.account.application.core.services.account;

import static com.jbh.account.application.core.mappers.AccountMapper.toDTO;
import static com.jbh.account.domain.utils.JbhMoneyUtils.JBH_ZERO;

import com.jbh.account.application.core.dto.AccountDTO;
import com.jbh.account.application.core.dto.MonthlyBalanceDTO;
import com.jbh.account.application.core.dto.MovementDTO;
import com.jbh.account.application.core.mappers.MovementMapper;
import com.jbh.account.application.core.ports.output.AccountRepository;
import com.jbh.account.domain.entity.AccountDomain;
import com.jbh.account.domain.entity.AccountMovementDomain;
import com.jbh.account.domain.exceptions.AccountBusinessException;
import com.jbh.account.domain.exceptions.BusinessDomainExceptionType;
import com.jbh.account.domain.exceptions.GenericSpecificationException;
import com.jbh.account.domain.vo.AccountId;
import com.jbh.account.domain.vo.AccountPK;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AccountServiceImpl implements AccountService {

  private final AccountRepository accountRepo;

  private final Logger log = LoggerFactory.getLogger(AccountServiceImpl.class);

  public AccountServiceImpl(final AccountRepository accountRepo) {
    this.accountRepo = accountRepo;
  }

  @Override
  public Optional<AccountDTO> findByUserAndAccountId(final UUID userId, final AccountId accountId) {
    return accountRepo.findByUserAndAccountId(userId, accountId);
  }

  @Override
  public AccountDTO findAccountOrThrow(final AccountId accountId) {
    return findByAccountId(accountId)
        .orElseThrow(() -> new IllegalArgumentException("Account not found"));
  }

  private Optional<AccountDTO> findByAccountId(final AccountId accountId) {
    return accountRepo.findByAccountId(accountId);
  }

  @Override
  public AccountDTO save(final AccountDTO account) {
    return accountRepo.save(account);
  }

  @Override
  public AccountDTO save(final AccountDomain account) {
    return accountRepo.save(toDTO(account));
  }

  @Override
  public void updateClosingProfitBalances(
      final AccountId accountId,
      final BigDecimal closingBalance,
      final BigDecimal calculatedNetProfit) {

    final var accountDomain = findDomainOrThrow(accountId);
    accountDomain.setCurrentBalance(closingBalance);
    accountDomain.setCalculatedNetProfit(calculatedNetProfit);
    log.info(
        "Setting {} Net Profit and {} current Balance for account {}",
        calculatedNetProfit,
        closingBalance,
        accountId.value());
    save(accountDomain);
  }

  private AccountDomain findDomainOrThrow(final AccountId accountId) {
    final Optional<AccountDTO> accountDTO = findByAccountId(accountId);

    if (accountDTO.isEmpty()) {
      throw new GenericSpecificationException("Account not found");
    }

    return accountDTO.get().toDomain();
  }

  @Override
  public void updateClosingBalances(final AccountId accountId, final BigDecimal closingBalance) {
    final var accountDomain = findDomainOrThrow(accountId);
    accountDomain.setCurrentBalance(closingBalance);

    log.info("Setting {} current Balance for account {}", closingBalance, accountId.value());
    save(accountDomain);
  }

  @Override
  public boolean isFullyWithdrawn(final AccountId accountId) {
    final AccountDomain accountDomain = findDomainOrThrow(accountId);
    return accountDomain.isFullyWithdrawn();
  }

  /**
   * Dates from monthly balances are used at the end of month
   *
   * @param accountId Account ID
   * @param monthlyBalances
   */
  @Override
  public void updateWhenFullyWithdrawn(
      final AccountId accountId, final List<MonthlyBalanceDTO> monthlyBalances) {
    final var accountDomain = findDomainOrThrow(accountId);
    if (accountDomain.isFullyWithdrawn()) {
      log.info("Updating account {} when it's fully withdrawn", accountId);
      // CashFlows
      final YearMonth maxPeriod = YearMonth.now().plusMonths(1);
      final List<BigDecimal> cashFlows = new ArrayList<>();
      final List<LocalDate> monthlyPeriods = new ArrayList<>();
      for (final MonthlyBalanceDTO monthlyBalanceDTO : monthlyBalances) {
        if (monthlyBalanceDTO.period().isBefore(maxPeriod)) {
          cashFlows.add(
              monthlyBalanceDTO.movementBalance() == null
                  ? JBH_ZERO
                  : monthlyBalanceDTO.movementBalance().negate());
          monthlyPeriods.add(monthlyBalanceDTO.period().atEndOfMonth());
        }
      }
      accountDomain.setCalculatedMoneyGrowthRate(cashFlows, monthlyPeriods);
      save(accountDomain);
    }
  }

  @Override
  public AccountDTO syncByMovement(
      final AccountPK accountPK,
      final MovementDTO movement,
      final boolean isMonthOfficiallyReported)
      throws AccountBusinessException {

    final AccountDomain accountDomain = findDomainOrThrow(accountPK);
    syncAccountDomainBalanceByMovement(
        accountDomain, MovementMapper.toDomain(movement), isMonthOfficiallyReported);

    log.info("Account {} was synced by Movement.. {}", accountDomain.getName(), movement);

    return toDTO(accountDomain);
  }

  private void syncAccountDomainBalanceByMovement(
      final AccountDomain accountDomain,
      final AccountMovementDomain movement,
      final boolean isMonthOfficiallyReported)
      throws AccountBusinessException {
    accountDomain.syncBalancesByMovement(movement, isMonthOfficiallyReported);
  }

  @Override
  public AccountDTO syncByUploadedMovements(
      final AccountDomain accountDomain, final List<AccountMovementDomain> uploadedMovements)
      throws AccountBusinessException {

    if (uploadedMovements == null || uploadedMovements.isEmpty()) {
      throw new AccountBusinessException(BusinessDomainExceptionType.EMPTY_MOVEMENTS);
    }
    final List<AccountMovementDomain> filteredMovements =
        uploadedMovements.stream().filter(Objects::nonNull).toList();

    for (final AccountMovementDomain movement : filteredMovements) {
      syncAccountDomainBalanceByMovement(accountDomain, movement, false);
    }

    return toDTO(accountDomain);
  }

  private AccountDomain findDomainOrThrow(final AccountPK accountPK) {
    final UUID userId = accountPK.userId();
    final AccountId accountId = accountPK.accountId();

    if (userId == null) {
      log.error("User ID cannot be null");
      throw new GenericSpecificationException("User ID cannot be null");
    }

    final AccountDTO accountDTO =
        findByUserAndAccountId(userId, accountId)
            .orElseThrow(
                () -> {
                  log.error(
                      "Account not found for user: {} and account: {}", userId, accountId.value());
                  return new GenericSpecificationException("Account not found");
                });

    return accountDTO.toDomain();
  }
}

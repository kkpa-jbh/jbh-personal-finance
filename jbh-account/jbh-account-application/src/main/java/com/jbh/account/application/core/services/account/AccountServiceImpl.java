package com.jbh.account.application.core.services.account;

import static com.jbh.account.application.core.mappers.AccountMapper.toDTO;
import static com.jbh.commons.util.JbhMoneyUtils.JBH_ZERO;

import com.jbh.account.application.core.dto.MonthlyBalanceDTO;
import com.jbh.account.application.core.dto.MovementDTO;
import com.jbh.account.application.core.dto.ProductDTO;
import com.jbh.account.application.core.exceptions.BusinessApplicationExceptionType;
import com.jbh.account.application.core.mappers.MovementMapper;
import com.jbh.account.application.core.ports.output.AccountRepository;
import com.jbh.account.domain.entity.ProductDomain;
import com.jbh.account.domain.entity.ProductMovementDomain;
import com.jbh.account.domain.exceptions.BusinessDomainExceptionType;
import com.jbh.commons.exception.GenericSpecificationException;
import com.jbh.account.domain.vo.ProductId;
import com.jbh.account.domain.vo.ProductPK;
import com.jbh.commons.exception.BusinessException;
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
  public ProductDTO findByUserAndAccountId(final UUID userId, final ProductId accountId)
      throws BusinessException {

    if (userId == null) {
      log.error("User ID cannot be null");
      throw new GenericSpecificationException("User ID cannot be null");
    }

    if (accountId == null) {
      log.error("Account ID cannot be null");
      throw new GenericSpecificationException("Account ID cannot be null");
    }

    return accountRepo
        .findByUserAndAccountId(userId, accountId)
        .orElseThrow(
            () -> {
              log.error(
                  "Account not found for user: {} and account: {}", userId, accountId.value());

              return new BusinessException(BusinessApplicationExceptionType.PRODUCT_NOT_FOUND);
            });
  }

  @Override
  public ProductDTO findAccountOrThrow(final ProductId accountId) {
    return findByAccountId(accountId)
        .orElseThrow(() -> new IllegalArgumentException("Account not found"));
  }

  private Optional<ProductDTO> findByAccountId(final ProductId accountId) {
    return accountRepo.findByAccountId(accountId);
  }

  @Override
  public ProductDTO save(final ProductDTO account) {
    return accountRepo.save(account);
  }

  @Override
  public ProductDTO save(final ProductDomain account) {
    return accountRepo.save(toDTO(account));
  }

  @Override
  public void updateClosingProfitBalances(
      final ProductId accountId,
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

  private ProductDomain findDomainOrThrow(final ProductId accountId) {
    final Optional<ProductDTO> accountDTO = findByAccountId(accountId);

    if (accountDTO.isEmpty()) {
      throw new GenericSpecificationException("Account not found");
    }

    return accountDTO.get().toDomain();
  }

  @Override
  public void updateClosingBalances(final ProductId accountId, final BigDecimal closingBalance) {
    final var accountDomain = findDomainOrThrow(accountId);
    accountDomain.setCurrentBalance(closingBalance);

    log.info("Setting {} current Balance for account {}", closingBalance, accountId.value());
    save(accountDomain);
  }

  @Override
  public boolean isFullyWithdrawn(final ProductId accountId) {
    final ProductDomain accountDomain = findDomainOrThrow(accountId);
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
      final ProductId accountId, final List<MonthlyBalanceDTO> monthlyBalances) {
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
  public ProductDTO syncByMovement(
      final ProductPK accountPK,
      final MovementDTO movement,
      final boolean isMonthOfficiallyReported)
      throws BusinessException {

    final ProductDomain accountDomain = findDomainOrThrow(accountPK);
    syncAccountDomainBalanceByMovement(
        accountDomain, MovementMapper.toDomain(movement), isMonthOfficiallyReported);

    log.info("Account {} was synced by Movement.. {}", accountDomain.getName(), movement);

    return toDTO(accountDomain);
  }

  private void syncAccountDomainBalanceByMovement(
      final ProductDomain accountDomain,
      final ProductMovementDomain movement,
      final boolean isMonthOfficiallyReported)
      throws BusinessException {
    accountDomain.syncBalancesByMovement(movement, isMonthOfficiallyReported);
  }

  @Override
  public ProductDTO syncByUploadedMovements(
      final ProductDomain accountDomain, final List<ProductMovementDomain> uploadedMovements)
      throws BusinessException {

    if (uploadedMovements == null || uploadedMovements.isEmpty()) {
      throw new BusinessException(BusinessDomainExceptionType.EMPTY_MOVEMENTS);
    }
    final List<ProductMovementDomain> filteredMovements =
        uploadedMovements.stream().filter(Objects::nonNull).toList();

    for (final ProductMovementDomain movement : filteredMovements) {
      try {
        syncAccountDomainBalanceByMovement(accountDomain, movement, false);
      } catch (final BusinessException ex) {
        log.error("Error syncing account movement by movement {}", movement);
        throw ex;
      }
    }

    return toDTO(accountDomain);
  }

  private ProductDomain findDomainOrThrow(final ProductPK accountPK) throws BusinessException {
    final UUID userId = accountPK.userId();
    final ProductId accountId = accountPK.accountId();

    final ProductDTO accountDTO = findByUserAndAccountId(userId, accountId);

    return accountDTO.toDomain();
  }
}

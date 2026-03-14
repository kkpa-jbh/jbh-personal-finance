package com.jbh.finance.application.feature.product.services;

import static com.jbh.commons.util.JbhMoneyUtils.JBH_ZERO;
import static com.jbh.finance.application.feature.product.mappers.ProductMapper.toDTO;

import com.jbh.commons.exception.BusinessException;
import com.jbh.commons.exception.GenericSpecificationException;
import com.jbh.finance.application.feature.monthlybalance.dto.MonthlyBalanceDTO;
import com.jbh.finance.application.feature.movement.dto.MovementDTO;
import com.jbh.finance.application.feature.movement.mappers.MovementMapper;
import com.jbh.finance.application.feature.product.dto.ProductDTO;
import com.jbh.finance.application.feature.product.ports.output.ProductRepository;
import com.jbh.finance.application.shared.exceptions.BusinessApplicationExceptionType;
import com.jbh.finance.domain.movement.MovementDomain;
import com.jbh.finance.domain.movement.vo.ProcessMovementOptionsVO;
import com.jbh.finance.domain.product.ProductDomain;
import com.jbh.finance.domain.product.vo.ProductId;
import com.jbh.finance.domain.product.vo.ProductPK;
import com.jbh.finance.domain.shared.exceptions.BusinessDomainExceptionType;
import com.jbh.finance.domain.shared.vo.EntityOperationVO;
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

public class ProductLifecycleServiceImpl implements ProductLifecycleService {

  private final ProductRepository accountRepo;

  private final Logger log = LoggerFactory.getLogger(ProductLifecycleServiceImpl.class);

  public ProductLifecycleServiceImpl(final ProductRepository accountRepo) {
    this.accountRepo = accountRepo;
  }

  @Override
  public ProductDTO findOrThrowByUserAndProductId(final UUID userId, final ProductId productId)
      throws BusinessException {

    if (userId == null) {
      log.error("User ID cannot be null");
      throw new GenericSpecificationException("User ID cannot be null");
    }

    if (productId == null) {
      log.error("Account ID cannot be null");
      throw new GenericSpecificationException("Account ID cannot be null");
    }

    return accountRepo
        .findByUserAndProductId(userId, productId)
        .orElseThrow(
            () -> {
              log.error(
                  "Account not found for user: {} and productDTO: {}", userId, productId.value());

              return new BusinessException(BusinessApplicationExceptionType.PRODUCT_NOT_FOUND);
            });
  }

  @Override
  public ProductDTO findOrThrowByIdProductId(final ProductId productId) {
    return findProductById(productId)
        .orElseThrow(() -> new IllegalArgumentException("Account not found"));
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
        "Setting {} Net Profit and {} current Balance for productDTO {}",
        calculatedNetProfit,
        closingBalance,
        accountId.value());
    save(accountDomain);
  }

  private ProductDomain findDomainOrThrow(final ProductId accountId) {
    final Optional<ProductDTO> accountDTO = findProductById(accountId);

    if (accountDTO.isEmpty()) {
      throw new GenericSpecificationException("Account not found");
    }

    return accountDTO.get().toDomain();
  }

  @Override
  public void updateClosingBalances(final ProductId accountId, final BigDecimal closingBalance) {
    final var accountDomain = findDomainOrThrow(accountId);
    accountDomain.setCurrentBalance(closingBalance);

    log.info("Setting {} current Balance for productDTO {}", closingBalance, accountId.value());
    save(accountDomain);
  }

  @Override
  public boolean isFullyWithdrawn(final ProductId accountId) {
    log.info("Checking It's product {} fully withdrawn ", accountId.value());
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
      log.info("Updating productDTO {} when it's fully withdrawn", accountId);
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
      final ProcessMovementOptionsVO movementOptions)
      throws BusinessException {

    final ProductDomain accountDomain = findDomainOrThrow(accountPK);
    syncAccountDomainBalanceByMovement(
        accountDomain, MovementMapper.toDomain(movement), movementOptions);

    log.info("Account {} was synced by Movement.. {}", accountDomain.getName(), movement);

    return toDTO(accountDomain);
  }

  private void syncAccountDomainBalanceByMovement(
      final ProductDomain accountDomain,
      final MovementDomain movement,
      final ProcessMovementOptionsVO movementOptions)
      throws BusinessException {
    accountDomain.syncBalancesByMovement(movement, movementOptions);
  }

  @Override
  public ProductDTO syncByUploadedMovements(
      final ProductDomain accountDomain, final List<MovementDomain> uploadedMovements)
      throws BusinessException {

    if (uploadedMovements == null || uploadedMovements.isEmpty()) {
      throw new BusinessException(BusinessDomainExceptionType.EMPTY_MOVEMENTS);
    }
    final List<MovementDomain> filteredMovements =
        uploadedMovements.stream().filter(Objects::nonNull).toList();

    final var movOptions = new ProcessMovementOptionsVO(false, EntityOperationVO.ADD);
    for (final MovementDomain movement : filteredMovements) {
      try {
        syncAccountDomainBalanceByMovement(accountDomain, movement, movOptions);
      } catch (final BusinessException ex) {
        log.error("Error syncing productDTO movement by movement {}", movement);
        throw ex;
      }
    }

    return toDTO(accountDomain);
  }

  @Override
  public List<ProductDTO> findActiveByUserId(final UUID userId) {
    return accountRepo.findActiveByUserId(userId);
  }

  @Override
  public Optional<ProductDTO> findProductById(final ProductId productId) {
    log.info("Find product by id {}", productId);
    return accountRepo.findByProductId(productId);
  }

  @Override
  public void deleteProduct(final ProductId productId) {
    log.info("Deleting product...");
    accountRepo.deleteById(productId);
    log.warn("Deleted product with id {}", productId);
  }

  private ProductDomain findDomainOrThrow(final ProductPK accountPK) throws BusinessException {
    final UUID userId = accountPK.userId();
    final ProductId accountId = accountPK.productId();

    final ProductDTO accountDTO = findOrThrowByUserAndProductId(userId, accountId);

    return accountDTO.toDomain();
  }
}

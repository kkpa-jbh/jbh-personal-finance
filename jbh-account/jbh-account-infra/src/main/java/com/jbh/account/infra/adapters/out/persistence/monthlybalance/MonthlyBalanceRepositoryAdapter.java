package com.jbh.account.infra.adapters.out.persistence.monthlybalance;

import com.jbh.account.application.accounts.ports.output.AccountMonthlyBalanceRepository;
import com.jbh.account.domain.vo.AccountId;
import com.jbh.account.domain.vo.AccountMonthlyBalanceDTO;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import java.time.YearMonth;
import java.util.List;
import java.util.Optional;
import org.apache.commons.collections4.CollectionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ApplicationScoped
@Transactional
public class MonthlyBalanceRepositoryAdapter implements AccountMonthlyBalanceRepository {

  private static final Logger LOGGER =
      LoggerFactory.getLogger(MonthlyBalanceRepositoryAdapter.class);
  @Inject MonthlyBalanceJPARepository jpaRepo;

  @Override
  public Optional<AccountMonthlyBalanceDTO> findByAccountIdYearAndMonth(
      final AccountId accountId, final Integer balanceYear, final Integer balanceMonth) {
    return jpaRepo
        .findByAccountIdYearAndMonth(accountId, balanceYear, balanceMonth)
        .map(AccountMonthlyBalanceJPAEntity::toDTO);
  }

  @Override
  @Transactional
  public void saveSingleMovement(final AccountMonthlyBalanceDTO accountMonthlyBalance) {
    final AccountMonthlyBalanceJPAEntity entity =
        AccountMonthlyBalanceJPAEntity.of(accountMonthlyBalance);

    if (entity.getId() == null) {
      jpaRepo.persist(entity);
    } else {
      // Use saveAndFlush for existing entities
      jpaRepo.getEntityManager().merge(entity);
      jpaRepo.getEntityManager().flush();
    }

    entity.toDTO();
  }

  @Override
  @Transactional
  // FIXME: Improve it to save all at once
  public List<AccountMonthlyBalanceDTO> saveMultiMovements(
      final List<AccountMonthlyBalanceDTO> accountMonthlyBalance) {
    try {
      final List<AccountMonthlyBalanceJPAEntity> movementsToPersist =
          accountMonthlyBalance.stream().map(AccountMonthlyBalanceJPAEntity::of).toList();

      if (CollectionUtils.isNotEmpty(movementsToPersist)) {
        final List<AccountMonthlyBalanceJPAEntity> newMovementsToPersist =
            movementsToPersist.stream().filter(m -> m.getId() == null).toList();
        final List<AccountMonthlyBalanceJPAEntity> existingMovementsToPersist =
            movementsToPersist.stream().filter(m -> m.getId() != null).toList();
        if (CollectionUtils.isNotEmpty(newMovementsToPersist)) {
          LOGGER.info("Saving new monthly balances {}", newMovementsToPersist.size());
          jpaRepo.persist(newMovementsToPersist);
        }
        if (CollectionUtils.isNotEmpty(existingMovementsToPersist)) {
          LOGGER.info("Updating existing monthly balances {}", existingMovementsToPersist.size());
          existingMovementsToPersist.forEach(m -> saveSingleMovement(m.toDTO()));
        }
      }

    } catch (final Exception e) {
      throw new IllegalStateException("Error persisting monthly balance " + e.getMessage(), e);
    }

    return accountMonthlyBalance;
  }

  @Override
  public List<AccountMonthlyBalanceDTO> findNextBalancesFromPeriodInclusive(
      final AccountId accountId, final YearMonth currentPeriod) {
    return jpaRepo.findNextBalancesFromPeriodInclusive(accountId, currentPeriod).stream()
        .map(AccountMonthlyBalanceJPAEntity::toDTO)
        .toList();
  }
}

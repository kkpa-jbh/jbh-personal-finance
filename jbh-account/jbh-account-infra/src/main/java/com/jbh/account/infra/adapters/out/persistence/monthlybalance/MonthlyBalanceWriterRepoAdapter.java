package com.jbh.account.infra.adapters.out.persistence.monthlybalance;

import com.jbh.account.application.accounts.ports.output.monthlybalance.AccountMonthlyBalanceWriterRepository;
import com.jbh.account.domain.vo.AccountMonthlyBalanceDTO;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import java.util.List;
import org.apache.commons.collections4.CollectionUtils;

@Transactional
public class MonthlyBalanceWriterRepoAdapter implements AccountMonthlyBalanceWriterRepository {
  @Inject MonthlyBalanceJPARepository jpaRepo;

  @Override
  // FIXME: Improve it to save all at once
  public List<AccountMonthlyBalanceDTO> saveMultiBalances(
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
          jpaRepo.persist(newMovementsToPersist);
        }
        if (CollectionUtils.isNotEmpty(existingMovementsToPersist)) {
          existingMovementsToPersist.forEach(m -> saveBalance(m.toDTO()));
        }
      }

    } catch (final Exception e) {
      throw new IllegalStateException("Error persisting monthly balance " + e.getMessage(), e);
    }

    return accountMonthlyBalance;
  }

  @Override
  public void saveBalance(final AccountMonthlyBalanceDTO accountMonthlyBalance) {
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
}

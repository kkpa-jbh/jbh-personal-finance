package com.jbh.products.infra.adapters.out.persistence.monthlybalance;

import com.jbh.products.application.core.dto.MonthlyBalanceDTO;
import com.jbh.products.application.core.ports.output.monthlybalance.AccountMonthlyBalanceWriterRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.transaction.Transactional;
import java.util.List;
import org.apache.commons.collections4.CollectionUtils;

@Transactional
@ApplicationScoped
@Named("monthlyBalanceWriterJPAAdapter")
public class MonthlyBalanceWriterRepoAdapter implements AccountMonthlyBalanceWriterRepository {
  @Inject MonthlyBalanceJPARepository jpaRepo;

  @Override
  // FIXME: Improve it to save all at once
  public List<MonthlyBalanceDTO> saveMultiBalances(
      final List<MonthlyBalanceDTO> accountMonthlyBalance) {
    try {
      final List<MonthlyBalanceJPAEntity> movementsToPersist =
          accountMonthlyBalance.stream().map(MonthlyBalanceJPAEntity::of).toList();

      if (CollectionUtils.isNotEmpty(movementsToPersist)) {
        final List<MonthlyBalanceJPAEntity> newMovementsToPersist =
            movementsToPersist.stream().filter(m -> m.getId() == null).toList();
        final List<MonthlyBalanceJPAEntity> existingMovementsToPersist =
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
  public void saveBalance(final MonthlyBalanceDTO accountMonthlyBalance) {
    final MonthlyBalanceJPAEntity entity = MonthlyBalanceJPAEntity.of(accountMonthlyBalance);

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

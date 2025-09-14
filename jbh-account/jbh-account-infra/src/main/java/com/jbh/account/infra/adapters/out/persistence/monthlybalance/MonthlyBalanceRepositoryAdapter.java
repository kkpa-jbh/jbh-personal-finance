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

@ApplicationScoped
public class MonthlyBalanceRepositoryAdapter implements AccountMonthlyBalanceRepository {

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
  public AccountMonthlyBalanceDTO save(final AccountMonthlyBalanceDTO accountMonthlyBalance) {
    final AccountMonthlyBalanceJPAEntity entity =
        AccountMonthlyBalanceJPAEntity.of(accountMonthlyBalance);

    if (entity.getId() == null) {
      jpaRepo.persist(entity);
    } else {
      jpaRepo.getEntityManager().merge(entity);
    }

    return entity.toDTO();
  }

  @Override
  @Transactional
  // FIXME: This is not working when an exception is thrown (It's background transaction)
  public List<AccountMonthlyBalanceDTO> save(
      final List<AccountMonthlyBalanceDTO> accountMonthlyBalance) {
    try {
      jpaRepo.persist(
          accountMonthlyBalance.stream().map(AccountMonthlyBalanceJPAEntity::of).toList());
    } catch (final Exception e) {
      throw new IllegalStateException("Error persisting monthly balance", e);
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

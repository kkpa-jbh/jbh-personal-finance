package com.jbh.account.infra.adapters.out.persistence.account;

import static com.jbh.account.infra.LogSanitizer.sanitize;

import com.jbh.account.application.accounts.ports.output.AccountRepository;
import com.jbh.account.domain.vo.AccountDomainDTO;
import com.jbh.account.domain.vo.AccountId;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import java.util.Optional;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ApplicationScoped
public class AccountRepositoryAdapter implements AccountRepository {

  private static final Logger LOG = LoggerFactory.getLogger(AccountRepositoryAdapter.class);
  @Inject AccountJPARepository jpaRepo;

  @Override
  public Optional<AccountDomainDTO> findByAccountId(final UUID userId, final AccountId accountId) {
    final String input =
        String.format("AccountRepositoryAdapter.findByAccountId called %s - %s", userId, accountId);
    LOG.info(sanitize(input));

    if (userId == null || accountId == null || accountId.value() == null) {
      throw new IllegalArgumentException("User ID or Account ID cannot be null");
    }

    final Optional<AccountJPAEntity> foundAccount =
        jpaRepo.findByAccountId(userId, accountId.value());
    return foundAccount.map(AccountJPAEntity::toDTO);
  }

  @Override
  @Transactional
  public AccountDomainDTO save(final AccountDomainDTO account) {
    AccountJPAEntity entity = AccountJPAEntity.of(account);

    // If ID is null, it's a new entity - use persist
    // If ID is set, it's an existing entity - use merge
    if (entity.getId() == null) {
      jpaRepo.persist(entity);
    } else {
      entity = jpaRepo.getEntityManager().merge(entity);
    }

    return entity.toDTO();
  }

  /*
  public List<Account> findByUserId(UUID userId) {
    return jpaRepository.find("userId", userId)
        .list()
        .stream()
        .map(this::toDomain)
        .collect(toList());
  }

  public List<Account> findActiveByUserId(UUID userId) {
    return jpaRepository.find("userId = ?1 and isActive = true", userId)
        .list()
        .stream()
        .map(this::toDomain)
        .collect(toList());
  }

   */
}

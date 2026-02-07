package com.jbh.products.infra.adapters.out.persistence.account;

import com.jbh.products.application.feature.product.dto.ProductDTO;
import com.jbh.products.application.feature.product.ports.output.ProductRepository;
import com.jbh.products.domain.product.vo.ProductId;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ApplicationScoped
@Transactional
public class AccountRepositoryAdapter implements ProductRepository {

  private static final Logger LOG = LoggerFactory.getLogger(AccountRepositoryAdapter.class);
  @Inject AccountJPARepository jpaRepo;

  @Override
  public Optional<ProductDTO> findByUserAndProductId(final UUID userId, final ProductId accountId) {
    if (userId == null || accountId == null || accountId.value() == null) {
      throw new IllegalArgumentException("User ID or Account ID cannot be null");
    }

    final Optional<ProductJPAEntity> foundAccount =
        jpaRepo.findByAccountId(userId, accountId.value());
    return foundAccount.map(ProductJPAEntity::toDTO);
  }

  @Override
  public Optional<ProductDTO> findByProductId(final ProductId accountId) {
    LOG.info("Fetching Product {} from DB", accountId.value());
    final Optional<ProductJPAEntity> foundAccount = jpaRepo.findByAccountId(accountId.value());
    return foundAccount.map(ProductJPAEntity::toDTO);
  }

  @Override
  public List<ProductDTO> findActiveByUserId(final UUID userId) {
    if (userId == null) {
      throw new IllegalArgumentException("User ID cannot be null");
    }
    return jpaRepo.findActiveByUserId(userId).stream().map(ProductJPAEntity::toDTO).toList();
  }

  @Override
  @Transactional
  public ProductDTO save(final ProductDTO account) {
    ProductJPAEntity entity = ProductJPAEntity.toEntity(account);

    // If ID is null, it's a new entity - use persist
    // If ID is set, it's an existing entity - use merge
    if (entity.getId() == null) {
      jpaRepo.persist(entity);
    } else {
      entity = jpaRepo.getEntityManager().merge(entity);
    }

    return entity.toDTO();
  }

  @Transactional
  @Override
  public void deleteById(final ProductId productId) {
    final Optional<ProductJPAEntity> foundAccount = jpaRepo.findByAccountId(productId.value());

    if (foundAccount.isEmpty()) {
      throw new IllegalArgumentException("Product ID not found to delete it");
    }

    jpaRepo.delete(foundAccount.get());
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

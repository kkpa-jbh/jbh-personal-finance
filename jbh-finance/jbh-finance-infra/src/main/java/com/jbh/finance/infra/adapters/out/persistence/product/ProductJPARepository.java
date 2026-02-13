package com.jbh.finance.infra.adapters.out.persistence.product;

import io.quarkus.hibernate.orm.panache.PanacheRepository;
import io.quarkus.panache.common.Parameters;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.persistence.PersistenceUnit;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@ApplicationScoped
@PersistenceUnit(name = "finance")
public class ProductJPARepository implements PanacheRepository<ProductJPAEntity> {

  public Optional<ProductJPAEntity> findByProductId(final UUID userId, final UUID accountID) {
    return find(
            "userId = :userId and id = :productId",
            Parameters.with("userId", userId).and("productId", accountID))
        .firstResultOptional();
  }

  public Optional<ProductJPAEntity> findByProductId(final UUID accountId) {
    return find("id", accountId).firstResultOptional();
  }

  public List<ProductJPAEntity> findActiveByUserId(final UUID userId) {
    return find(
            "userId = :userId and isActive = :active",
            Parameters.with("userId", userId).and("active", true))
        .list();
  }
}

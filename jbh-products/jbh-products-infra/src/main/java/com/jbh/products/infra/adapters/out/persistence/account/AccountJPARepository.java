package com.jbh.products.infra.adapters.out.persistence.account;

import io.quarkus.hibernate.orm.panache.PanacheRepository;
import io.quarkus.panache.common.Parameters;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.persistence.PersistenceUnit;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@ApplicationScoped
@PersistenceUnit(name = "finance")
public class AccountJPARepository implements PanacheRepository<ProductJPAEntity> {

  public Optional<ProductJPAEntity> findByAccountNumber(final String accountNumber) {
    return find("accountNumber", accountNumber).firstResultOptional();
  }

  public List<ProductJPAEntity> findActiveAccounts() {
    return find("isActive", true).list();
  }

  public List<ProductJPAEntity> findByAccountType(final String accountType) {
    return find(
            "accountType = :type and isActive = :active",
            Parameters.with("type", accountType).and("active", true))
        .list();
  }

  public Optional<ProductJPAEntity> findByAccountId(final UUID userId, final UUID accountID) {
    return find(
            "userId = :userId and id = :accountId",
            Parameters.with("userId", userId).and("accountId", accountID))
        .firstResultOptional();
  }

  public Optional<ProductJPAEntity> findByAccountId(final UUID accountId) {
    return find("id", accountId).firstResultOptional();
  }

  public List<ProductJPAEntity> findActiveByUserId(final UUID userId) {
    return find(
            "userId = :userId and isActive = :active",
            Parameters.with("userId", userId).and("active", true))
        .list();
  }
}

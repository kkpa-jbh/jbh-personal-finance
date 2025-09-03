package com.jbh.account.infra.persistence;

import io.quarkus.hibernate.orm.panache.PanacheRepository;
import io.quarkus.panache.common.Parameters;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.persistence.PersistenceUnit;
import java.util.List;
import java.util.Optional;

@ApplicationScoped
@PersistenceUnit(name = "acctmgmt")
public class AccountJPARepository implements PanacheRepository<AccountJPAEntity> {

  public Optional<AccountJPAEntity> findByAccountNumber(String accountNumber) {
    return find("accountNumber", accountNumber).firstResultOptional();
  }

  public List<AccountJPAEntity> findActiveAccounts() {
    return find("isActive", true).list();
  }

  public List<AccountJPAEntity> findByAccountType(String accountType) {
    return find("accountType = :type and isActive = :active",
        Parameters.with("type", accountType)
            .and("active", true)).list();
  }
}
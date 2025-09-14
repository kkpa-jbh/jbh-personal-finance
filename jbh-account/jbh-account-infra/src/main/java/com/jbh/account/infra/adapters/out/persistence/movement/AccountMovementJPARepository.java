package com.jbh.account.infra.adapters.out.persistence.movement;

import io.quarkus.hibernate.orm.panache.PanacheRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.persistence.PersistenceUnit;

@ApplicationScoped
@PersistenceUnit(name = "acctmgmt")
public class AccountMovementJPARepository implements PanacheRepository<AccountMovementJPAEntity> {}

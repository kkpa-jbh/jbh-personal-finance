package com.jbh.preferences.infra.adapters.out.persistence;

import io.quarkus.hibernate.orm.panache.PanacheRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.persistence.PersistenceUnit;
import java.util.Optional;
import java.util.UUID;

@ApplicationScoped
@PersistenceUnit(name = "userprefs")
public class UserPreferencesJPARepository implements PanacheRepository<UserPreferencesJPAEntity> {

  public Optional<UserPreferencesJPAEntity> findByUserId(final UUID userId) {
    return find("userId", userId).firstResultOptional();
  }

  public boolean existsByUserId(final UUID userId) {
    return count("userId", userId) > 0;
  }
}

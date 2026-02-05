package com.jbh.preferences.infra.adapters.out.persistence;

import io.quarkus.hibernate.orm.panache.PanacheRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.persistence.PersistenceUnit;
import java.util.Optional;
import java.util.UUID;

@ApplicationScoped
@PersistenceUnit(name = "preferences")
public class TeamPreferencesJPARepository implements PanacheRepository<TeamPreferencesJPAEntity> {

  public Optional<TeamPreferencesJPAEntity> findByTeamId(final UUID teamId) {
    return find("teamId", teamId).firstResultOptional();
  }

  public boolean existsByTeamId(final UUID teamId) {
    return count("teamId", teamId) > 0;
  }
}

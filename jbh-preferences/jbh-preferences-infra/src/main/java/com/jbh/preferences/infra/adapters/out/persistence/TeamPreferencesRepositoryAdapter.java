package com.jbh.preferences.infra.adapters.out.persistence;

import com.jbh.preferences.application.core.dto.TeamPreferencesDTO;
import com.jbh.preferences.application.core.ports.output.TeamPreferencesRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import java.util.Optional;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ApplicationScoped
@SuppressWarnings("PMD.ConfusingTernary")
public class TeamPreferencesRepositoryAdapter implements TeamPreferencesRepository {

  private static final Logger LOG = LoggerFactory.getLogger(TeamPreferencesRepositoryAdapter.class);

  @Inject TeamPreferencesJPARepository jpaRepository;

  @Override
  public Optional<TeamPreferencesDTO> findByTeamId(final UUID teamId) {
    if (teamId == null) {
      throw new IllegalArgumentException("Team ID cannot be null");
    }
    final Optional<TeamPreferencesJPAEntity> found = jpaRepository.findByTeamId(teamId);
    return found.map(TeamPreferencesJPAEntity::toDTO);
  }

  @Override
  @Transactional
  public TeamPreferencesDTO save(final TeamPreferencesDTO preferences) {
    if (preferences == null) {
      throw new IllegalArgumentException("Preferences cannot be null");
    }

    TeamPreferencesJPAEntity entity = TeamPreferencesJPAEntity.toEntity(preferences);

    if (!jpaRepository.existsByTeamId(entity.getTeamId())) {
      jpaRepository.persist(entity);
      LOG.debug("Created new preferences for team: {}", entity.getTeamId());
    } else {
      entity = jpaRepository.getEntityManager().merge(entity);
      LOG.debug("Updated preferences for team: {}", entity.getTeamId());
    }

    return entity.toDTO();
  }

  @Override
  public boolean existsByTeamId(final UUID teamId) {
    if (teamId == null) {
      throw new IllegalArgumentException("Team ID cannot be null");
    }
    return jpaRepository.existsByTeamId(teamId);
  }
}

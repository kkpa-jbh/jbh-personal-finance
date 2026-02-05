package com.jbh.preferences.infra.adapters.out.persistence;

import com.jbh.preferences.application.core.dto.UserPreferencesDTO;
import com.jbh.preferences.application.core.ports.output.UserPreferencesRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import java.util.Optional;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ApplicationScoped
@SuppressWarnings("PMD.ConfusingTernary")
public class UserPreferencesRepositoryAdapter implements UserPreferencesRepository {

  private static final Logger LOG = LoggerFactory.getLogger(UserPreferencesRepositoryAdapter.class);

  @Inject UserPreferencesJPARepository jpaRepository;

  @Override
  public Optional<UserPreferencesDTO> findByUserId(final UUID userId) {
    if (userId == null) {
      throw new IllegalArgumentException("User ID cannot be null");
    }
    final Optional<UserPreferencesJPAEntity> found = jpaRepository.findByUserId(userId);
    return found.map(UserPreferencesJPAEntity::toDTO);
  }

  @Override
  @Transactional
  public UserPreferencesDTO save(final UserPreferencesDTO preferences) {
    if (preferences == null) {
      throw new IllegalArgumentException("Preferences cannot be null");
    }

    UserPreferencesJPAEntity entity = UserPreferencesJPAEntity.toEntity(preferences);

    if (!jpaRepository.existsByUserId(entity.getUserId())) {
      jpaRepository.persist(entity);
      LOG.debug("Created new preferences for user: {}", entity.getUserId());
    } else {
      entity = jpaRepository.getEntityManager().merge(entity);
      LOG.debug("Updated preferences for user: {}", entity.getUserId());
    }

    return entity.toDTO();
  }

  @Override
  public boolean existsByUserId(final UUID userId) {
    if (userId == null) {
      throw new IllegalArgumentException("User ID cannot be null");
    }
    return jpaRepository.existsByUserId(userId);
  }
}

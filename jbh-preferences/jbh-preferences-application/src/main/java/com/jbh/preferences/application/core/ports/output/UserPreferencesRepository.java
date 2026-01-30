package com.jbh.preferences.application.core.ports.output;

import com.jbh.preferences.application.core.dto.UserPreferencesDTO;
import java.util.Optional;
import java.util.UUID;

public interface UserPreferencesRepository {

  Optional<UserPreferencesDTO> findByUserId(UUID userId);

  UserPreferencesDTO save(UserPreferencesDTO preferences);

  boolean existsByUserId(UUID userId);
}

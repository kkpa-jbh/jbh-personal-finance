package com.jbh.preferences.application.core.ports.output;

import com.jbh.preferences.application.core.dto.TeamPreferencesDTO;
import java.util.Optional;
import java.util.UUID;

public interface TeamPreferencesRepository {

  Optional<TeamPreferencesDTO> findByTeamId(UUID teamId);

  TeamPreferencesDTO save(TeamPreferencesDTO preferences);

  boolean existsByTeamId(UUID teamId);
}

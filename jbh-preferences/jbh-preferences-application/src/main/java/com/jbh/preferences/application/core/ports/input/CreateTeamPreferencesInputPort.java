package com.jbh.preferences.application.core.ports.input;

import com.jbh.commons.exception.BusinessException;
import com.jbh.preferences.application.core.dto.TeamPreferencesDTO;
import com.jbh.preferences.application.core.services.TeamPreferencesService;
import java.util.UUID;

public class CreateTeamPreferencesInputPort {

  private final TeamPreferencesService preferencesService;

  public CreateTeamPreferencesInputPort(final TeamPreferencesService preferencesService) {
    this.preferencesService = preferencesService;
  }

  public TeamPreferencesDTO execute(final UUID teamId, final UUID creatorUserId)
      throws BusinessException {
    return preferencesService.createDefaultPreferences(teamId, creatorUserId);
  }
}
